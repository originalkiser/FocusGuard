package com.focusguard.service

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.focusguard.domain.model.FilterResult
import com.focusguard.domain.usecase.EvaluateCallUseCase
import com.focusguard.util.AutoReplyManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Android 10+ call screening without requiring the default dialer role.
 * The system binds this service for every incoming call and waits for respondToCall().
 * If we don't respond within a few seconds the system allows the call through automatically.
 *
 * OEM note: On some Samsung/Xiaomi devices, aggressive battery killers may unbind this
 * service before the call arrives. Whitelist FocusGuard in battery settings to prevent this.
 */
@AndroidEntryPoint
class FocusGuardCallScreeningService : CallScreeningService() {

    @Inject lateinit var evaluateCallUseCase: EvaluateCallUseCase
    @Inject lateinit var autoReplyManager: AutoReplyManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: run {
            // Unknown number — allow through; user can create a pattern rule later
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        scope.launch {
            try {
                val result = evaluateCallUseCase.evaluate(number)
                val response = buildResponse(result, callDetails)

                // Send auto-reply before responding so the SMS goes out even for rejected calls
                if (result is FilterResult.Block && result.autoReply != null) {
                    autoReplyManager.sendSms(number, result.autoReply)
                }

                respondToCall(callDetails, response)
                Log.d(TAG, "Call from $number → ${result::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating call from $number — allowing through", e)
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private fun buildResponse(result: FilterResult, details: Call.Details): CallResponse =
        when (result) {
            is FilterResult.Block -> CallResponse.Builder()
                .setRejectCall(true)
                .setDisallowCall(false)   // false = send to voicemail; true = silent drop
                .setSkipCallLog(false)    // keep in call log so the user can see what was blocked
                .setSkipNotification(true) // no missed-call notification
                .build()
            else -> CallResponse.Builder().build()
        }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object { private const val TAG = "FocusGuard/CallScreen" }
}

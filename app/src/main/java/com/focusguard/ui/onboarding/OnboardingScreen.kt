package com.focusguard.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusguard.util.PermissionHelper

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var permStatuses by remember {
        mutableStateOf(PermissionHelper.getPermissionStatuses(context))
    }

    val runtimeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permStatuses = PermissionHelper.getPermissionStatuses(context)
    }

    // Runtime permissions we can request via the launcher
    val runtimePermissions = buildList {
        add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            add(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Icon(
            Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Welcome to FocusGuard",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Take control of who can reach you and when — without changing your default phone or SMS app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Text("Grant Permissions", style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(permStatuses, key = { it.name }) { perm ->
                OnboardingPermRow(
                    status = perm,
                    onRequest = {
                        if (perm.settingsIntent != null) {
                            context.startActivity(perm.settingsIntent)
                        } else {
                            runtimeLauncher.launch(runtimePermissions.toTypedArray())
                        }
                    },
                    onRefresh = { permStatuses = PermissionHelper.getPermissionStatuses(context) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                // Grant runtime permissions first if not done
                val missing = runtimePermissions.filter {
                    !PermissionHelper.hasPermission(context, it)
                }
                if (missing.isNotEmpty()) {
                    runtimeLauncher.launch(missing.toTypedArray())
                }
                viewModel.completeOnboarding()
                onComplete()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            val coreGranted = PermissionHelper.hasPermission(context, Manifest.permission.READ_CONTACTS)
            Text(if (coreGranted) "Get Started" else "Continue (some permissions pending)")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "You can grant additional permissions later in Settings.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingPermRow(
    status: PermissionHelper.PermissionStatus,
    onRequest: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (status.isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (status.isGranted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(status.name, style = MaterialTheme.typography.labelLarge)
                Text(
                    status.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!status.isGranted) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    onRequest()
                    onRefresh()
                }) { Text("Grant") }
            }
        }
    }
}

package com.focusguard.domain.model

sealed class FilterResult {
    data object Allow : FilterResult()
    data class Block(
        val ruleId: Long,
        val ruleName: String,
        val autoReply: String? = null
    ) : FilterResult()
    data object PassThrough : FilterResult()  // no matching rule; system default applies
}

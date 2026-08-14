package com.github.schaka.janitorr.config

data class UnmonitorAfterWatch(
    val enabled: Boolean = false,
    val webhookSecret: String = "",
) {
    init {
        require(!enabled || webhookSecret.isNotBlank()) {
            "application.unmonitor-after-watch.webhook-secret must be set when unmonitor-after-watch is enabled"
        }
    }
}

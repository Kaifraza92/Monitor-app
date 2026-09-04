package com.example.automation

enum class AutomationState(val displayName: String, val isTerminal: Boolean = false) {
    IDLE("Idle"),
    WAITING_FOR_SCHEDULE("Waiting for Schedule"),
    OPENING_MEET("Opening Meet URL"),
    WAITING_FOR_MEET_UI("Waiting for Meet UI"),
    DETECTING_JOIN("Detecting Join Control"),
    ENSURING_AV_MUTED("Ensuring Mic & Camera OFF"),
    JOINING("Joining Meeting"),
    VERIFYING_JOINED("Verifying Joined State"),
    ACTIVE("Meeting Active"),
    WAITING_FOR_END_TIME("Waiting for End Time"),
    LEAVING("Leaving Meeting"),
    VERIFYING_LEFT("Verifying Session Ended"),
    COMPLETED("Completed Successfully", isTerminal = true),

    // Failure states
    NETWORK_ERROR("Network Error", isTerminal = true),
    AUTH_REQUIRED("Authentication Required", isTerminal = true),
    JOIN_BUTTON_NOT_FOUND("Join Button Not Found", isTerminal = true),
    JOIN_FAILED("Join Action Failed", isTerminal = true),
    SESSION_TERMINATED("Session Terminated Prematurely", isTerminal = true),
    ACCESSIBILITY_DISABLED("Accessibility Service Disabled", isTerminal = true),
    BATTERY_RESTRICTION("Battery Optimization Restriction", isTerminal = true),
    UNKNOWN_ERROR("Unknown Error", isTerminal = true);

    val isFailure: Boolean
        get() = this in setOf(
            NETWORK_ERROR, AUTH_REQUIRED, JOIN_BUTTON_NOT_FOUND, JOIN_FAILED,
            SESSION_TERMINATED, ACCESSIBILITY_DISABLED, BATTERY_RESTRICTION, UNKNOWN_ERROR
        )
}

sealed class AutomationResult {
    data class Success(val message: String, val durationMs: Long = 0) : AutomationResult()
    data class Failure(val state: AutomationState, val reason: String) : AutomationResult()
}

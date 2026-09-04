package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeetAutomationAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceActive.value = true
        instance = this
        Log.i(TAG, "MeetAutomationAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event processing hook for active window changes
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName.contains("google", ignoreCase = true) ||
            packageName.contains("meet", ignoreCase = true) ||
            packageName.contains("chrome", ignoreCase = true) ||
            packageName.contains("browser", ignoreCase = true)
        ) {
            _lastTargetAppEvent.value = System.currentTimeMillis()
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "MeetAutomationAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "MeetAutomationAccessibilityService destroyed")
    }

    /**
     * Attempts to find and click the Join / Ask to join button in Google Meet.
     * Searches recursively across text, content description, and view hierarchy.
     */
    fun attemptJoin(): JoinAttemptResult {
        val rootNode = rootInActiveWindow ?: return JoinAttemptResult.NoWindowRoot

        // Step 1: Mute microphone and camera if available on preview screen
        ensureMicAndCameraMuted(rootNode)

        // Step 2: Search for join triggers
        val joinTerms = listOf(
            "join now",
            "ask to join",
            "join meeting",
            "join with a code",
            "join"
        )

        for (term in joinTerms) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(term)
            for (node in nodes) {
                if (performClickOnNodeOrParent(node)) {
                    return JoinAttemptResult.Clicked(term)
                }
            }
        }

        // Search by content description
        val clickedByDesc = findAndClickByPredicate(rootNode) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            val text = node.text?.toString()?.lowercase() ?: ""
            (desc.contains("join now") || desc.contains("ask to join") || desc == "join") ||
                    (text.contains("join now") || text.contains("ask to join"))
        }

        return if (clickedByDesc) {
            JoinAttemptResult.Clicked("Matched by predicate/description")
        } else {
            JoinAttemptResult.NotFound
        }
    }

    /**
     * Attempts to find and click Leave / End Call button.
     */
    fun attemptLeave(): LeaveAttemptResult {
        val rootNode = rootInActiveWindow ?: return LeaveAttemptResult.NoWindowRoot

        val leaveTerms = listOf(
            "leave call",
            "leave meeting",
            "end call",
            "hang up",
            "leave"
        )

        for (term in leaveTerms) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(term)
            for (node in nodes) {
                if (performClickOnNodeOrParent(node)) {
                    return LeaveAttemptResult.Clicked(term)
                }
            }
        }

        // Search by content description (Google Meet typically has "Leave call" contentDescription)
        val clicked = findAndClickByPredicate(rootNode) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            desc.contains("leave call") || desc.contains("end call") || desc.contains("hang up")
        }

        return if (clicked) {
            LeaveAttemptResult.Clicked("Matched by content description")
        } else {
            LeaveAttemptResult.NotFound
        }
    }

    /**
     * Inspects preview screen and turns off microphone and camera if currently on.
     */
    private fun ensureMicAndCameraMuted(rootNode: AccessibilityNodeInfo) {
        // Mute mic if button says "Turn off microphone"
        findAndClickByPredicate(rootNode) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            desc.contains("turn off microphone") || desc.contains("mute microphone")
        }

        // Turn off camera if button says "Turn off camera"
        findAndClickByPredicate(rootNode) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            desc.contains("turn off camera") || desc.contains("stop video")
        }
    }

    /**
     * Check if meeting appears to be currently in active joined state.
     */
    fun checkIsJoined(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val leaveNodes = rootNode.findAccessibilityNodeInfosByText("Leave call")
        if (leaveNodes.isNotEmpty()) return true

        // Check for in-call icons/descriptions like "In-call messages", "People", "Leave call"
        var inCallFound = false
        traverseNodes(rootNode) { node ->
            val desc = node.contentDescription?.toString()?.lowercase() ?: ""
            if (desc.contains("leave call") || desc.contains("in-call") || desc.contains("meeting details")) {
                inCallFound = true
            }
        }
        return inCallFound
    }

    private fun findAndClickByPredicate(
        node: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        if (predicate(node)) {
            if (performClickOnNodeOrParent(node)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickByPredicate(child, predicate)) {
                return true
            }
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 5) {
            if (current.isClickable) {
                val success = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            current = current.parent
            depth++
        }
        return false
    }

    private fun traverseNodes(node: AccessibilityNodeInfo, action: (AccessibilityNodeInfo) -> Unit) {
        action(node)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseNodes(child, action)
        }
    }

    sealed class JoinAttemptResult {
        data class Clicked(val trigger: String) : JoinAttemptResult()
        data object NotFound : JoinAttemptResult()
        data object NoWindowRoot : JoinAttemptResult()
    }

    sealed class LeaveAttemptResult {
        data class Clicked(val trigger: String) : LeaveAttemptResult()
        data object NotFound : LeaveAttemptResult()
        data object NoWindowRoot : LeaveAttemptResult()
    }

    companion object {
        private const val TAG = "MeetA11yService"
        var instance: MeetAutomationAccessibilityService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastTargetAppEvent = MutableStateFlow<Long>(0L)
        val lastTargetAppEvent: StateFlow<Long> = _lastTargetAppEvent.asStateFlow()
    }
}

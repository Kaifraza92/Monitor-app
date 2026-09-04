package com.example.automation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InAppMeetingSession(
    val executionId: Long = -1L,
    val scheduleName: String = "",
    val url: String = "",
    val isActive: Boolean = false,
    val isJoined: Boolean = false,
    val isMuted: Boolean = true,
    val progress: Int = 0,
    val title: String = "Google Meet",
    val statusText: String = "Idle",
    val startedAt: Long = 0L
)

class InAppMeetSessionManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _sessionState = MutableStateFlow(InAppMeetingSession())
    val sessionState: StateFlow<InAppMeetingSession> = _sessionState.asStateFlow()

    private var activeWebView: WebView? = null

    @Synchronized
    fun getOrCreateWebView(hostContext: Context = context): WebView {
        if (activeWebView == null) {
            val wv = WebView(hostContext.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                configureWebView(this)
            }
            activeWebView = wv
        }
        return activeWebView!!
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowContentAccess = true
            allowFileAccess = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            // Desktop/CrOS Chrome User-Agent: Forces Google Meet to render web client without redirecting to Play Store or external app
            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                // Strictly deny camera and microphone capture!
                // This hardware/browser-level refusal ensures mic and camera stay completely disabled.
                Log.d("InAppMeetSession", "Browser requested resources: ${request?.resources?.joinToString()}, strictly denying.")
                request?.deny()
                _sessionState.value = _sessionState.value.copy(isMuted = true)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                _sessionState.value = _sessionState.value.copy(progress = newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    _sessionState.value = _sessionState.value.copy(title = title)
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Prevent jumping to external market or intent schemes
                if (url.startsWith("intent:") || url.startsWith("market:") || url.contains("play.google.com/store")) {
                    Log.d("InAppMeetSession", "Intercepted external intent/market URL, preventing external launch: $url")
                    return true
                }
                // Allow in-app navigation for Google Meet & accounts
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d("InAppMeetSession", "Page finished loading: $url")
                _sessionState.value = _sessionState.value.copy(
                    statusText = "Meet web client loaded in-app. Running background presence..."
                )
                triggerAutoJoinAndMute()
            }
        }
    }

    fun startSession(executionId: Long, url: String, scheduleName: String) {
        mainHandler.post {
            _sessionState.value = InAppMeetingSession(
                executionId = executionId,
                scheduleName = scheduleName,
                url = url,
                isActive = true,
                isJoined = false,
                isMuted = true,
                progress = 0,
                title = scheduleName,
                statusText = "Opening inside app and running in background...",
                startedAt = System.currentTimeMillis()
            )

            val wv = getOrCreateWebView()
            Log.i("InAppMeetSession", "Loading Google Meet in-app: $url")
            wv.loadUrl(url)
        }
    }

    fun triggerAutoJoinAndMute() {
        mainHandler.post {
            val wv = activeWebView ?: return@post
            val script = """
                (function() {
                    var actions = [];
                    var buttons = document.querySelectorAll('button, div[role="button"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var b = buttons[i];
                        var label = (b.getAttribute('aria-label') || '').toLowerCase();
                        var text = (b.innerText || '').toLowerCase();
                        
                        // Turn off microphone if active
                        if (label.indexOf('turn off microphone') !== -1 || label.indexOf('mute microphone') !== -1) {
                            b.click();
                            actions.push('mic_muted');
                        }
                        // Turn off camera if active
                        if (label.indexOf('turn off camera') !== -1 || label.indexOf('turn off video') !== -1) {
                            b.click();
                            actions.push('cam_muted');
                        }
                        // Click join/ask to join
                        if (label.indexOf('ask to join') !== -1 || label.indexOf('join now') !== -1 || 
                            text === 'ask to join' || text === 'join now' || text.indexOf('join meeting') !== -1) {
                            b.click();
                            actions.push('join_clicked');
                        }
                    }
                    return actions.join(',');
                })();
            """.trimIndent()

            wv.evaluateJavascript(script) { result ->
                if (result != null && result.contains("join_clicked")) {
                    _sessionState.value = _sessionState.value.copy(
                        isJoined = true,
                        statusText = "In-app join command sent. Session active in app and background."
                    )
                }
            }
        }
    }

    fun reloadSession() {
        mainHandler.post {
            val url = _sessionState.value.url
            if (url.isNotBlank()) {
                activeWebView?.loadUrl(url)
            }
        }
    }

    fun leaveSession() {
        mainHandler.post {
            val wv = activeWebView
            if (wv != null) {
                val leaveScript = """
                    (function() {
                        var buttons = document.querySelectorAll('button, div[role="button"]');
                        for (var i = 0; i < buttons.length; i++) {
                            var label = (buttons[i].getAttribute('aria-label') || '').toLowerCase();
                            if (label.indexOf('leave call') !== -1 || label.indexOf('hang up') !== -1 || label.indexOf('leave meeting') !== -1) {
                                buttons[i].click();
                                return true;
                            }
                        }
                        return false;
                    })();
                """.trimIndent()
                wv.evaluateJavascript(leaveScript) {
                    mainHandler.postDelayed({
                        wv.loadUrl("about:blank")
                        _sessionState.value = InAppMeetingSession(isActive = false, statusText = "Session ended")
                    }, 1200L)
                }
            } else {
                _sessionState.value = InAppMeetingSession(isActive = false, statusText = "Session ended")
            }
        }
    }
}

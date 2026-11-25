package com.gymtrainer.app.ui.videos

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gymtrainer.app.data.YouTube
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.MotionPrefs
import com.gymtrainer.app.ui.theme.AppTheme
import java.net.URLEncoder

/**
 * Free YouTube search inside the app (no API key): YouTube's mobile search page in a WebView.
 * As soon as the user taps a video, its id is handed back with [onPicked] so the app can play
 * it in its own player and save it to the exercise.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoSearchScreen(query: String, onBack: () -> Unit, onPicked: (String) -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    var picked by remember { mutableStateOf(false) }

    fun check(url: String?): Boolean {
        val id = url?.takeIf { "/watch" in it || "/shorts/" in it }?.let { YouTube.parseId(it) } ?: return false
        if (!picked) { picked = true; onPicked(id) }
        return true
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar("Search YouTube", onBack)
        Text("Tap any video to play it here and save it to this exercise.",
            style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted,
            modifier = Modifier.padding(horizontal = Gutter).padding(bottom = 6.dp))
        if (progress in 0.01f..0.99f) LinearProgressIndicator(progress = { progress }, Modifier.fillMaxWidth())
        Box(Modifier.fillMaxSize()) {
            if (!MotionPrefs.allowNetwork) return@Box
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress / 100f }
                        }
                        webViewClient = object : WebViewClient() {
                            // Full page loads (e.g. opening a result in a new page).
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                if (check(request.url.toString())) return true
                                val host = request.url.host.orEmpty()
                                // Stay on YouTube; don't wander off to other sites or app stores.
                                return !(host.endsWith("youtube.com") || host.endsWith("google.com") || host.endsWith("gstatic.com"))
                            }
                            // YouTube's mobile site is a single-page app: tapping a result changes the URL without a page load.
                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                if (check(url)) { view.stopLoading(); if (view.canGoBack()) view.goBack() }
                            }
                        }
                        loadUrl("https://m.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8"))
                    }
                },
                onRelease = { it.stopLoading(); it.destroy() }
            )
        }
    }
}

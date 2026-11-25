package com.gymtrainer.app.ui.videos

import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import android.webkit.WebViewClient
import android.webkit.WebView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gymtrainer.app.data.YouTube
import com.gymtrainer.app.ui.components.MotionPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private object ThumbCache {
    val cache = LruCache<String, ImageBitmap>(60)
}

/** YouTube thumbnail loaded over the network (cached), with a play badge. */
@Composable
fun VideoThumbnail(videoId: String, modifier: Modifier = Modifier, badge: Dp = 34.dp) {
    val bmp by produceState(ThumbCache.cache.get(videoId), videoId) {
        if (value != null || !MotionPrefs.allowNetwork) return@produceState
        value = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(YouTube.thumbnail(videoId)).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.inputStream.use { BitmapFactory.decodeStream(it)?.asImageBitmap() }.also { c.disconnect() }
            }.getOrNull()
        }?.also { ThumbCache.cache.put(videoId, it) }
    }
    Box(modifier.background(Color(0xFF1B1F27)), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.size(badge).clip(CircleShape).background(Color(0xE6FF0033)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(badge * 0.62f))
        }
    }
}


/**
 * In-app YouTube player: YouTube's own embed page inside a WebView, set up the way Google requires
 * for apps (https://developers.google.com/youtube/terms/required-minimum-functionality):
 *  - the page is loaded with base URL https://<applicationId>, so the embed gets that HTTP Referer,
 *  - referrer policy strict-origin-when-cross-origin, DOM storage and YouTube's cookies allowed.
 * No JavaScript bridge or "ready" handshake is needed: YouTube's own play button and error
 * messages show inside the frame. Links out of the player open the YouTube app.
 * Falls back to a thumbnail when network use is disabled (tests).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbed(videoId: String, modifier: Modifier = Modifier) {
    if (!MotionPrefs.allowNetwork) {
        VideoThumbnail(videoId, modifier, badge = 56.dp)
        return
    }
    val context = LocalContext.current
    val origin = embedOrigin(context)
    var offline by remember(videoId) { mutableStateOf(false) }
    var fullscreenView by remember { mutableStateOf<android.view.View?>(null) }
    var fullscreenCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    fun load(web: WebView) {
        offline = false
        web.tag = videoId
        web.loadDataWithBaseURL(origin, playerHtml(videoId, origin), "text/html", "utf-8", null)
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true            // YouTube's player script needs localStorage
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
                            fullscreenView = view; fullscreenCallback = callback
                        }
                        override fun onHideCustomView() { fullscreenView = null; fullscreenCallback = null }
                    }
                    webViewClient = object : WebViewClient() {
                        // Anything that tries to leave the player page (title, channel, "Watch on YouTube") opens YouTube.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                            return true
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame || request.url.host?.contains("youtube") == true && request.url.path?.startsWith("/embed") == true) {
                                offline = true
                            }
                        }
                    }
                    load(this)
                }
            },
            update = { web -> if (web.tag != videoId) load(web) },
            onRelease = { web -> web.stopLoading(); web.destroy() }
        )
        if (offline) {
            Box(Modifier.fillMaxSize().background(Color(0xF0101218)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("Couldn't reach YouTube. Check your connection.", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFF0033))
                            .clickable { openOnYouTube(context, videoId) }.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text(" Watch on YouTube", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }

    // YouTube's fullscreen button: show the video over everything until it's closed (or Back).
    fullscreenView?.let { v ->
        Dialog(
            onDismissRequest = { fullscreenCallback?.onCustomViewHidden(); fullscreenView = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            AndroidView(factory = { (v.parent as? android.view.ViewGroup)?.removeView(v); v },
                modifier = Modifier.fillMaxSize().background(Color.Black))
        }
    }
}

/** The page hosting YouTube's embed iframe. [videoId] is sanitised; [origin] is the app's https id. */
internal fun playerHtml(videoId: String, origin: String): String {
    val id = videoId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }.take(20)
    val o = java.net.URLEncoder.encode(origin, "UTF-8")
    return """<!DOCTYPE html><html><head>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<meta name="referrer" content="strict-origin-when-cross-origin">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}
iframe{position:absolute;top:0;left:0;width:100%;height:100%;border:0}</style></head>
<body><iframe src="https://www.youtube.com/embed/$id?playsinline=1&rel=0&iv_load_policy=3&origin=$o&widget_referrer=$o"
 referrerpolicy="strict-origin-when-cross-origin"
 allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
 allowfullscreen></iframe></body></html>"""
}

/** The app's own id as an https origin, e.g. https://com.gymtrainer.app (what YouTube requires from apps). */
fun embedOrigin(context: Context): String = "https://${context.packageName}"

/** Opens the video in the YouTube app, or the browser if it isn't installed. */
fun openOnYouTube(context: Context, videoId: String) {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse(YouTube.watchUrl(videoId)))
    runCatching { context.startActivity(app) }.recoverCatching { context.startActivity(web) }
}

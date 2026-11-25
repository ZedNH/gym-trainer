package com.gymtrainer.app.ui.videos

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
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gymtrainer.app.data.YouTube
import com.gymtrainer.app.ui.components.MotionPrefs
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
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
 * Official YouTube IFrame player (via android-youtube-player). Cues [videoId]; the user
 * presses play. Falls back to a thumbnail when network use is disabled (tests).
 *
 * YouTube rejects embeds that don't identify their host ("Error 152/153"). The player
 * is initialised manually so its origin (also the WebView base URL, i.e. the Referer) is
 * this app's id. If YouTube still refuses a video, a "Watch on YouTube" fallback appears.
 */
@Composable
fun YouTubeEmbed(videoId: String, modifier: Modifier = Modifier) {
    if (!MotionPrefs.allowNetwork) {
        VideoThumbnail(videoId, modifier, badge = 56.dp)
        return
    }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var view by remember { mutableStateOf<YouTubePlayerView?>(null) }
    var failed by remember(videoId) { mutableStateOf(false) }
    var reason by remember(videoId) { mutableStateOf("") }

    Box(modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                YouTubePlayerView(ctx).also { v ->
                    view = v
                    v.enableAutomaticInitialization = false
                    lifecycle.addObserver(v)
                    val options = IFramePlayerOptions.Builder()
                        .controls(1).rel(0).ivLoadPolicy(3).fullscreen(0)
                        .origin(embedOrigin(ctx))
                        .build()
                    // YouTube's consent / bot checks need its cookies; Android WebViews block third-party cookies by default.
                    allowYouTubeCookies(v)
                    v.initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            player = youTubePlayer
                            youTubePlayer.cueVideo(videoId, 0f)
                        }
                        override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            reason = describe(error)
                            failed = true
                        }
                    }, options)
                }
            }
        )
        if (failed) {
            Box(Modifier.fillMaxSize().background(Color(0xF0101218)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("This video can't play inside the app.", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    if (reason.isNotEmpty()) Text(reason, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
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
    LaunchedEffect(videoId, player) { failed = false; reason = ""; player?.cueVideo(videoId, 0f) }
    // If YouTube never answers (blocked network, broken WebView), don't leave a blank box.
    LaunchedEffect(videoId) {
        kotlinx.coroutines.delay(15_000)
        if (player == null && !failed) { reason = "The YouTube player didn't load (no response in 15 s)."; failed = true }
    }
    DisposableEffect(Unit) {
        onDispose {
            view?.let { lifecycle.removeObserver(it); it.release() }
        }
    }
}

/** The app's own id as an https origin, e.g. https://com.gymtrainer.app (what YouTube expects from apps). */
fun embedOrigin(context: Context): String = "https://${context.packageName}"

/** Opens the video in the YouTube app, or the browser if it isn't installed. */
fun openOnYouTube(context: Context, videoId: String) {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    val web = Intent(Intent.ACTION_VIEW, Uri.parse(YouTube.watchUrl(videoId)))
    runCatching { context.startActivity(app) }.recoverCatching { context.startActivity(web) }
}

/** Readable reason for the fallback screen, so a screenshot tells us exactly what YouTube said. */
private fun describe(error: PlayerConstants.PlayerError): String = when (error) {
    PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER -> "The creator doesn't allow this video in other apps (error 101/150)."
    PlayerConstants.PlayerError.VIDEO_NOT_FOUND -> "Video removed or private (error 100)."
    PlayerConstants.PlayerError.HTML_5_PLAYER -> "YouTube player error (5)."
    PlayerConstants.PlayerError.INVALID_PARAMETER_IN_REQUEST -> "Invalid video request (2)."
    else -> "YouTube refused the in-app player (${error.name})."
}

/** Finds the player's WebView and lets it keep YouTube's cookies. */
private fun allowYouTubeCookies(root: android.view.View) {
    val cookies = android.webkit.CookieManager.getInstance()
    cookies.setAcceptCookie(true)
    fun walk(v: android.view.View) {
        if (v is android.webkit.WebView) cookies.setAcceptThirdPartyCookies(v, true)
        if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
    }
    walk(root)
}

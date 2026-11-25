package com.gymtrainer.app.ui.videos

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
 */
@Composable
fun YouTubeEmbed(videoId: String, modifier: Modifier = Modifier) {
    if (!MotionPrefs.allowNetwork) {
        VideoThumbnail(videoId, modifier, badge = 56.dp)
        return
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var view by remember { mutableStateOf<YouTubePlayerView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            YouTubePlayerView(ctx).also { v ->
                view = v
                lifecycle.addObserver(v)
                v.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        player = youTubePlayer
                        youTubePlayer.cueVideo(videoId, 0f)
                    }
                })
            }
        }
    )
    LaunchedEffect(videoId, player) { player?.cueVideo(videoId, 0f) }
    DisposableEffect(Unit) {
        onDispose {
            view?.let { lifecycle.removeObserver(it); it.release() }
        }
    }
}

package com.gymtrainer.app.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.ui.theme.Accent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Global switch for looping demo frames (turned off in UI tests so the clock can idle). */
object MotionPrefs {
    @Volatile var animateDemos = true
    /** Off in unit tests: no network thumbnails and no WebView-based player. */
    @Volatile var allowNetwork = true
}

private object FrameCache {
    val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
}

/**
 * Shows an exercise's bundled photos. With [animate], the start and end frames
 * alternate to demonstrate the movement. Exercises without photos get an icon.
 */
@Composable
fun ExerciseImage(
    exercise: ExerciseEntity?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    thumbnail: Boolean = false,
    corner: Dp = 14.dp
) {
    val context = LocalContext.current
    val count = exercise?.imageCount ?: 0
    var frame by remember(exercise?.id) { mutableIntStateOf(0) }

    if (animate && count > 1 && MotionPrefs.animateDemos) {
        LaunchedEffect(exercise?.id) {
            while (true) {
                delay(900)
                frame = (frame + 1) % count
            }
        }
    }

    val bitmap by produceState<ImageBitmap?>(initialValue = null, exercise?.id, frame, thumbnail) {
        val ex = exercise
        if (ex == null || count == 0) { value = null; return@produceState }
        val key = "${ex.id}/$frame/$thumbnail"
        val cached = FrameCache.cache.get(key)
        value = cached ?: withContext(Dispatchers.IO) {
            runCatching {
                context.assets.open("ex/${ex.imageOf.ifEmpty { ex.id }}/$frame.webp").use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = if (thumbnail) 2 else 1 }
                    BitmapFactory.decodeStream(stream, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }?.also { FrameCache.cache.put(key, it) }
    }

    // Illustrated placeholder for moves without photos: tinted by movement type.
    val (icon, tint) = when (exercise?.category) {
        "stretching" -> Icons.Filled.SelfImprovement to com.gymtrainer.app.ui.theme.Brand.Teal
        "cardio", "plyometrics" -> Icons.Filled.DirectionsRun to com.gymtrainer.app.ui.theme.Brand.Orange
        else -> Icons.Filled.FitnessCenter to com.gymtrainer.app.ui.theme.Brand.Blue
    }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(
                if (count > 0) Brush.linearGradient(listOf(Color.White, Color.White))
                else Brush.linearGradient(listOf(tint.copy(alpha = 0.10f), tint.copy(alpha = 0.22f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, contentDescription = exercise?.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier.size(if (thumbnail) 40.dp else 120.dp).clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = exercise?.name, tint = tint, modifier = Modifier.size(if (thumbnail) 24.dp else 68.dp))
            }
        }
    }
}

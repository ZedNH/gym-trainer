package com.gymtrainer.app.ui.videos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.data.ExerciseEntity
import com.gymtrainer.app.data.Tutorial
import com.gymtrainer.app.data.VideoSource
import com.gymtrainer.app.data.YouTube
import com.gymtrainer.app.ui.components.AppCard
import com.gymtrainer.app.ui.components.AppTextField
import com.gymtrainer.app.ui.components.DetailTopBar
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.components.PillButton
import com.gymtrainer.app.ui.components.SecondaryButton
import com.gymtrainer.app.ui.components.SectionHeader
import com.gymtrainer.app.ui.components.Tag
import com.gymtrainer.app.ui.rememberRepository
import com.gymtrainer.app.ui.rememberSettings
import com.gymtrainer.app.ui.rememberVideos
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand
import kotlinx.coroutines.launch

@Composable
fun VideoTutorialScreen(exerciseId: String, onBack: () -> Unit) {
    val repo = rememberRepository()
    val videos = rememberVideos()
    val settings = rememberSettings()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exercise by produceState<ExerciseEntity?>(null, exerciseId) { value = repo.exercise(exerciseId) }
    val verified = remember(exerciseId) { videos.verified(exerciseId) }
    val mine by videos.observeMine(exerciseId).collectAsState(initial = emptyList())
    var results by remember { mutableStateOf<List<Tutorial>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var query by remember(exercise) { mutableStateOf(exercise?.let { "${it.name} proper form tutorial" } ?: "") }
    var showAdd by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Tutorial?>(null) }
    val current = selected ?: (verified + mine).firstOrNull()
    val hasKey = settings.youtubeApiKey.isNotBlank()

    fun openExternal(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun runSearch() {
        searching = true; searchError = null
        scope.launch {
            videos.search(query, settings.youtubeApiKey)
                .onSuccess { results = it; if (it.isEmpty()) searchError = "No embeddable videos found." }
                .onFailure { searchError = "Search failed. Check your API key and connection." }
            searching = false
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        DetailTopBar(exercise?.name ?: "Video tutorials", onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Player
            item {
                if (current != null) {
                    Column(Modifier.padding(horizontal = Gutter)) {
                        YouTubeEmbed(current.videoId, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(20.dp)))
                        Spacer(Modifier.height(10.dp))
                        Text(current.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(current.channel, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, modifier = Modifier.weight(1f))
                            TextButton(onClick = { openExternal(YouTube.watchUrl(current.videoId)) }) {
                                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("YouTube")
                            }
                        }
                    }
                } else {
                    AppCard(Modifier.padding(horizontal = Gutter)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.SmartDisplay, Brand.Red, 48.dp)
                            Column(Modifier.padding(start = 14.dp)) {
                                Text("No saved tutorial yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text("Search YouTube or paste a link to save one here.", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
                            }
                        }
                    }
                }
            }

            // Actions
            item {
                Row(Modifier.padding(horizontal = Gutter), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton("Paste link", { showAdd = true }, Modifier.weight(1f).height(48.dp), icon = Icons.Filled.Link)
                    SecondaryButton("Browse", { openExternal(YouTube.searchUrl(query)) }, Modifier.weight(1f).height(48.dp), icon = Icons.Filled.OpenInNew)
                }
            }

            if (verified.isNotEmpty()) {
                item { SectionHeader("Verified tutorials") }
                items(verified, key = { "v_" + it.videoId }) { t -> VideoRow(t, t == current, onPlay = { selected = t }) }
            }
            if (mine.isNotEmpty()) {
                item { SectionHeader("My videos") }
                items(mine, key = { "m_" + it.rowId }) { t ->
                    VideoRow(t, t == current, onPlay = { selected = t }) {
                        IconButton(onClick = { scope.launch { t.rowId?.let { videos.remove(it) }; if (selected == t) selected = null } }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = AppTheme.colors.textFaint)
                        }
                    }
                }
            }

            // Search
            item { SectionHeader("Find more") }
            if (hasKey) {
                item {
                    Row(Modifier.padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
                        AppTextField(query, { query = it }, "Search YouTube", Modifier.weight(1f),
                            leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = AppTheme.colors.textMuted) })
                        Spacer(Modifier.width(10.dp))
                        PillButton(if (searching) "…" else "Search", { runSearch() }, enabled = !searching && query.isNotBlank())
                    }
                }
                searchError?.let { e -> item { Text(e, Modifier.padding(horizontal = Gutter), color = AppTheme.colors.danger, style = MaterialTheme.typography.bodySmall) } }
                items(results, key = { "s_" + it.videoId }) { t ->
                    VideoRow(t, t == current, onPlay = { selected = t }) {
                        IconButton(onClick = { scope.launch { videos.save(exerciseId, t) } }) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                item {
                    AppCard(Modifier.padding(horizontal = Gutter)) {
                        Text("Search inside the app", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Add your own free YouTube Data API key in Profile → Video search to find and play tutorials for any of the 880+ exercises here. Or tap Browse to search in the YouTube app.",
                            style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted
                        )
                    }
                }
            }
            item {
                Text("Videos are streamed with YouTube's official player and belong to their creators. Nothing is downloaded.",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textFaint, modifier = Modifier.padding(horizontal = Gutter))
            }
        }
    }

    if (showAdd) {
        AddLinkDialog(
            onDismiss = { showAdd = false },
            onAdd = { link, done ->
                val id = YouTube.parseId(link)
                if (id == null) { done("That doesn't look like a YouTube link."); return@AddLinkDialog }
                scope.launch {
                    val t = videos.lookup(id)
                    if (t == null) done("Couldn't load that video — it may be private or not embeddable.")
                    else { videos.save(exerciseId, t); selected = t; done(null); showAdd = false }
                }
            }
        )
    }
}

@Composable
private fun VideoRow(t: Tutorial, playing: Boolean, onPlay: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    AppCard(Modifier.padding(horizontal = Gutter), onClick = onPlay, padding = PaddingValues(10.dp),
        color = if (playing) MaterialTheme.colorScheme.primaryContainer else AppTheme.colors.card) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VideoThumbnail(t.videoId, Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)), badge = 28.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(t.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(t.channel, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
                if (t.source == VideoSource.VERIFIED) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = AppTheme.colors.success, modifier = Modifier.padding(end = 4.dp))
                        Tag("Verified", AppTheme.colors.success)
                    }
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun AddLinkDialog(onDismiss: () -> Unit, onAdd: (String, (String?) -> Unit) -> Unit) {
    var link by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.card,
        title = { Text("Add a YouTube video") },
        text = {
            Column {
                AppTextField(link, { link = it; error = null }, "https://youtu.be/…")
                error?.let { Text(it, color = AppTheme.colors.danger, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            TextButton(enabled = link.isNotBlank() && !busy, onClick = {
                busy = true
                onAdd(link) { err -> busy = false; error = err }
            }) { Icon(Icons.Filled.Add, contentDescription = null); Text(if (busy) " Adding…" else " Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

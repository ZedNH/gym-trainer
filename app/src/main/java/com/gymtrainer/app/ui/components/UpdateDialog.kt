package com.gymtrainer.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gymtrainer.app.data.AppUpdate
import com.gymtrainer.app.data.UpdateInstaller
import com.gymtrainer.app.data.Updates
import com.gymtrainer.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class Step { OFFER, DOWNLOADING, NEEDS_PERMISSION, INSTALLING, FAILED }

/**
 * "Update available" dialog. Downloads the signed APK inside the app with progress, verifies it,
 * asks once for "Install unknown apps" if needed, then opens Android's install screen.
 */
@Composable
fun UpdateDialog(update: AppUpdate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf(Step.OFFER) }
    var progress by remember { mutableFloatStateOf(0f) }
    var error by remember { mutableStateOf("") }
    var apk by remember { mutableStateOf<File?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun installNow(file: File) {
        if (!UpdateInstaller.canInstall(context)) { step = Step.NEEDS_PERMISSION; return }
        step = Step.INSTALLING
        runCatching { UpdateInstaller.install(context, file) }.onFailure { error = "Couldn't open the installer."; step = Step.FAILED }
    }

    fun start() {
        step = Step.DOWNLOADING; progress = 0f
        job = scope.launch {
            runCatching {
                val file = UpdateInstaller.download(context, update.apkUrl) { p -> progress = p }
                withContext(Dispatchers.IO) { UpdateInstaller.verify(context, file) }?.let { error(it) }
                file
            }.onSuccess { apk = it; installNow(it) }
                .onFailure { if (step == Step.DOWNLOADING) { error = it.message ?: "Download failed. Check your connection."; step = Step.FAILED } }
        }
    }

    // Coming back from the "Install unknown apps" settings screen: continue automatically.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (step == Step.NEEDS_PERMISSION && UpdateInstaller.canInstall(context)) apk?.let { installNow(it) }
    }

    AlertDialog(
        onDismissRequest = { if (step != Step.DOWNLOADING) onDismiss() },
        containerColor = AppTheme.colors.card,
        title = {
            Text(
                when (step) {
                    Step.DOWNLOADING -> "Downloading v${update.version}…"
                    Step.NEEDS_PERMISSION -> "One-time permission"
                    Step.INSTALLING -> "Ready to install"
                    Step.FAILED -> "Update didn't finish"
                    Step.OFFER -> "Update available · v${update.version}"
                },
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                when (step) {
                    Step.OFFER -> {
                        Text("You have v${Updates.currentVersion(context)}. Your workouts and settings are kept.",
                            style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                        if (update.notes.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(update.notes.replace("**", "").replace("## ", "").replace("# ", "").trim(),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Step.DOWNLOADING -> {
                        LinearMeter(if (progress < 0) 0.5f else progress, Modifier.fillMaxWidth(), height = 8.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(if (progress < 0) "Downloading…" else "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    }
                    Step.NEEDS_PERMISSION -> Text(
                        "Android asks you to allow Gym Trainer to install updates. Tap Allow, switch on " +
                            "\"Allow from this source\", then come back. The install continues by itself.",
                        style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    Step.INSTALLING -> Text("Tap Update on Android's screen to finish. The app restarts on the new version.",
                        style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                    Step.FAILED -> Text(error, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                }
            }
        },
        confirmButton = {
            when (step) {
                Step.OFFER -> TextButton(onClick = { start() }) { Text("Download & install") }
                Step.DOWNLOADING -> {}
                Step.NEEDS_PERMISSION -> TextButton(onClick = {
                    runCatching { context.startActivity(UpdateInstaller.permissionIntent(context)) }
                }) { Text("Allow") }
                Step.INSTALLING -> TextButton(onClick = { apk?.let { installNow(it) } }) { Text("Open installer") }
                Step.FAILED -> TextButton(onClick = { start() }) { Text("Try again") }
            }
        },
        dismissButton = {
            when (step) {
                Step.OFFER -> TextButton(onClick = { Updates.skip(context, update.version); onDismiss() }) { Text("Skip this version") }
                Step.DOWNLOADING -> TextButton(onClick = { job?.cancel(); step = Step.OFFER }) { Text("Cancel") }
                Step.FAILED -> TextButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl))) }; onDismiss()
                }) { Text("Open on GitHub") }
                else -> TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

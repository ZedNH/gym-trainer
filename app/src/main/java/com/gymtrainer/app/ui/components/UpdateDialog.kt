package com.gymtrainer.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.data.AppUpdate
import com.gymtrainer.app.data.Updates
import com.gymtrainer.app.ui.theme.AppTheme

/** "Update available" dialog. Download opens the signed APK; Android's installer does the rest. */
@Composable
fun UpdateDialog(update: AppUpdate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.card,
        title = { Text("Update available · v${update.version}", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                Text("You have v${Updates.currentVersion(context)}. Your workouts and settings are kept when you update.",
                    style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
                if (update.notes.isNotBlank()) {
                    Text("\n" + update.notes.replace("**", "").replace("## ", "").replace("# ", "").trim(),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("\nAfter the download finishes, tap it and allow installing from your browser if Android asks.",
                    style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.apkUrl))) }
                onDismiss()
            }) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = { Updates.skip(context, update.version); onDismiss() }) { Text("Skip this version") }
        }
    )
}

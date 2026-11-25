package com.gymtrainer.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a release APK into the app's private cache, checks it really is a newer
 * Gym Trainer signed with the same key, and hands it to Android's package installer.
 */
object UpdateInstaller {

    private fun dir(context: Context) = File(context.cacheDir, "updates").apply { mkdirs() }
    fun apkFile(context: Context) = File(dir(context), "GymTrainer-update.apk")

    /** Streams [url] to the cache, reporting progress 0..1 (or -1 when the size is unknown). */
    suspend fun download(context: Context, url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        dir(context).listFiles()?.forEach { it.delete() }
        val part = File(dir(context), "download.part")
        var conn = open(url)
        // GitHub redirects release assets to its CDN; follow up to 5 hops.
        repeat(5) {
            if (conn.responseCode in 300..399) {
                val next = conn.getHeaderField("Location") ?: error("Bad redirect")
                conn.disconnect(); conn = open(next)
            }
        }
        try {
            if (conn.responseCode != 200) error("Server returned ${conn.responseCode}")
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                part.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        onProgress(if (total > 0) done.toFloat() / total else -1f)
                    }
                }
            }
        } finally { conn.disconnect() }
        val apk = apkFile(context)
        if (!part.renameTo(apk)) error("Couldn't save the update")
        apk
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = false
        connectTimeout = 15000; readTimeout = 30000
        setRequestProperty("User-Agent", "GymTrainer-Android")
    }

    /** Null if [apk] is safe to install, otherwise a reason to show the user. */
    fun verify(context: Context, apk: File): String? {
        val pm = context.packageManager
        val archive = archiveInfo(pm, apk) ?: return "The download is damaged. Please try again."
        if (archive.packageName != context.packageName) return "This file isn't a Gym Trainer update."
        val installed = pm.getPackageInfo(context.packageName, signingFlag())
        if (versionCode(archive) <= versionCode(installed)) return "You already have this version or a newer one."
        val a = signatures(archive); val b = signatures(installed)
        // Android's installer also refuses a different key; some older versions can't read archive signatures.
        if (a.isNotEmpty() && b.isNotEmpty() && a != b) return "This update isn't signed by Gym Trainer, so it wasn't installed."
        return null
    }

    /** Android 8+: the user must allow this app to install updates once. */
    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** Opens the system "Update this app?" screen for the downloaded APK. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ---- package info helpers ----
    private fun signingFlag() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    private fun archiveInfo(pm: PackageManager, apk: File): PackageInfo? =
        if (Build.VERSION.SDK_INT >= 33) pm.getPackageArchiveInfo(apk.path, PackageManager.PackageInfoFlags.of(signingFlag().toLong()))
        else @Suppress("DEPRECATION") pm.getPackageArchiveInfo(apk.path, signingFlag())

    private fun versionCode(p: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else @Suppress("DEPRECATION") p.versionCode.toLong()

    private fun signatures(p: PackageInfo): Set<String> {
        val sigs = if (Build.VERSION.SDK_INT >= 28) p.signingInfo?.let {
            if (it.hasMultipleSigners()) it.apkContentsSigners else it.signingCertificateHistory
        } else @Suppress("DEPRECATION") p.signatures
        val md = MessageDigest.getInstance("SHA-256")
        return sigs.orEmpty().map { s -> md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) } }.toSet()
    }
}

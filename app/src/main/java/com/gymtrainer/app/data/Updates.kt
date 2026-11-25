package com.gymtrainer.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A newer release published on GitHub. */
data class AppUpdate(val version: String, val title: String, val notes: String, val apkUrl: String, val pageUrl: String)

/**
 * Checks the project's GitHub Releases for a newer signed APK. No account or key needed:
 * it reads the public "latest release" endpoint. Installing is done by Android's own installer.
 */
object Updates {
    const val REPO = "ZedNH/gym-trainer"
    const val RELEASES_PAGE = "https://github.com/$REPO/releases/latest"
    private const val CHECK_EVERY_MS = 12 * 60 * 60 * 1000L

    fun currentVersion(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "0"

    /** "6.0.1" > "6.0"; ignores a leading "v" and any suffix after a dash. */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").substringBefore('-')
            .split('.').map { it.toIntOrNull() ?: 0 }
        val r = parts(remote); val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** Parses GitHub's release JSON. Returns null when there's no APK asset. */
    fun parse(json: String): AppUpdate? {
        val o = JSONObject(json)
        val tag = o.optString("tag_name").ifBlank { return null }
        val assets = o.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) } ?: return null
        return AppUpdate(
            version = tag.removePrefix("v"),
            title = o.optString("name").ifBlank { "Gym Trainer $tag" },
            notes = o.optString("body"),
            apkUrl = apk.optString("browser_download_url"),
            pageUrl = o.optString("html_url").ifBlank { RELEASES_PAGE }
        )
    }

    /** Fetches the latest release; null if offline, up to date or anything goes wrong. */
    suspend fun check(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000; readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "GymTrainer-Android")
            }
            try {
                if (conn.responseCode != 200) return@runCatching null
                val update = parse(conn.inputStream.bufferedReader().use { it.readText() }) ?: return@runCatching null
                update.takeIf { isNewer(it.version, currentVersion(context)) }
            } finally { conn.disconnect() }
        }.getOrNull()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("gym_trainer_updates", Context.MODE_PRIVATE)

    /** True at most once every 12 hours, so the launch check stays light. */
    fun dueForCheck(context: Context): Boolean {
        val p = prefs(context)
        val now = System.currentTimeMillis()
        if (now - p.getLong("last_check", 0L) < CHECK_EVERY_MS) return false
        p.edit().putLong("last_check", now).apply()
        return true
    }

    fun isSkipped(context: Context, version: String) = prefs(context).getString("skipped", null) == version
    fun skip(context: Context, version: String) = prefs(context).edit().putString("skipped", version).apply()
}

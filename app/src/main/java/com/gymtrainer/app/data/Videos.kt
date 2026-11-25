package com.gymtrainer.app.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

enum class VideoSource { VERIFIED, MINE, SEARCH }

data class Tutorial(
    val videoId: String,
    val title: String,
    val channel: String,
    val source: VideoSource,
    val rowId: Long? = null
)

object YouTube {
    private val patterns = listOf(
        Regex("""(?:v=|youtu\.be/|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})"""),
        Regex("""^([A-Za-z0-9_-]{11})$""")
    )

    /** Extract an 11-character video id from a pasted link or bare id. */
    fun parseId(input: String): String? {
        val s = input.trim()
        for (p in patterns) p.find(s)?.let { return it.groupValues[1] }
        return null
    }

    fun thumbnail(id: String) = "https://i.ytimg.com/vi/$id/mqdefault.jpg"
    fun watchUrl(id: String) = "https://www.youtube.com/watch?v=$id"
    fun searchUrl(query: String) = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
}

/**
 * Exercise video tutorials:
 *  - VERIFIED: bundled in assets/videos.json, each confirmed embeddable via YouTube oEmbed;
 *  - MINE: links the user pasted (verified through oEmbed before saving);
 *  - SEARCH: optional YouTube Data API search with the user's own API key.
 * Playback always uses the official YouTube IFrame player; nothing is downloaded.
 */
class VideoRepository(private val assets: AssetManager, private val dao: VideoDao) {

    private val curated: Map<String, List<Tutorial>> by lazy {
        runCatching {
            val text = assets.open("videos.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val o = JSONObject(text)
            o.keys().asSequence().associateWith { key ->
                val arr = o.getJSONArray(key)
                (0 until arr.length()).map { i ->
                    val v = arr.getJSONObject(i)
                    Tutorial(v.getString("id"), v.getString("title"), v.getString("channel"), VideoSource.VERIFIED)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun verified(exerciseId: String): List<Tutorial> = curated[exerciseId].orEmpty()
    fun hasVerified(exerciseId: String) = curated.containsKey(exerciseId)
    fun verifiedCount() = curated.values.sumOf { it.size }

    fun observeMine(exerciseId: String): Flow<List<Tutorial>> = dao.observe(exerciseId).map { list ->
        list.map { Tutorial(it.videoId, it.title, it.channel, VideoSource.MINE, it.id) }
    }

    suspend fun save(exerciseId: String, t: Tutorial) =
        dao.insert(UserVideoEntity(exerciseId = exerciseId, videoId = t.videoId, title = t.title, channel = t.channel, addedAt = System.currentTimeMillis()))

    suspend fun remove(rowId: Long) = dao.delete(rowId)

    /** Look up title/channel for a pasted link. Null if the video doesn't exist or can't be embedded. */
    suspend fun lookup(videoId: String): Tutorial? = withContext(Dispatchers.IO) {
        runCatching {
            val url = "https://www.youtube.com/oembed?format=json&url=" + URLEncoder.encode(YouTube.watchUrl(videoId), "UTF-8")
            val json = JSONObject(httpGet(url))
            Tutorial(videoId, json.optString("title", "YouTube video"), json.optString("author_name", ""), VideoSource.MINE)
        }.getOrNull()
    }

    private fun httpGet(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000
        c.readTimeout = 15_000
        try {
            val code = c.responseCode
            if (code !in 200..299) {
                val msg = c.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("HTTP $code ${msg.take(200)}")
            }
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}

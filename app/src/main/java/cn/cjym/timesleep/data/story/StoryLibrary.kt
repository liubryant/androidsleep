/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.data.story

import android.content.Context
import cn.cjym.timesleep.data.model.StoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

class StoryLibrary(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    private val _stories = MutableStateFlow<List<StoryItem>>(emptyList())
    val stories: StateFlow<List<StoryItem>> = _stories.asStateFlow()

    private val prefs = appContext.getSharedPreferences("story_library", Context.MODE_PRIVATE)
    private val _favorites = MutableStateFlow(prefs.getStringSet("favorites", emptySet())!!.toSet())
    val favorites = _favorites.asStateFlow()
    private val _articlesEnabled = MutableStateFlow(false)
    val articlesEnabled = _articlesEnabled.asStateFlow()
    fun toggleFavorite(id: String) {
        val next = _favorites.value.toMutableSet().apply { if (!add(id)) remove(id) }.toSet()
        prefs.edit().putStringSet("favorites", next).apply()
        _favorites.value = next
    }
    suspend fun load() {
        if (_stories.value.isNotEmpty()) return
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                appContext.assets.open("StoryResources/stories_manifest.json").use { input ->
                    json.decodeFromString<List<StoryItem>>(input.bufferedReader().readText())
                        .sortedWith(
                            compareByDescending<StoryItem> { it.isBundled }
                                .thenBy { it.index },
                        )
                }
            }.getOrElse { emptyList() }
        }
        _stories.value = loaded
        val enabled = withContext(Dispatchers.IO) {
            runCatching {
                val connection = java.net.URL("https://www.cjym123.cn/api/info").openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 8000; connection.readTimeout = 8000
                try {
                    val obj = connection.inputStream.bufferedReader().use { json.parseToJsonElement(it.readText()).jsonObject }
                    check(obj["resultCode"]?.jsonPrimitive?.content == "200")
                    (obj["data"]?.jsonObject?.get("storySwitch")?.jsonPrimitive?.content == "1").also {
                        prefs.edit().putBoolean("articles_enabled", it).apply()
                    }
                } finally { connection.disconnect() }
            }.getOrDefault(prefs.getBoolean("articles_enabled", false))
        }
        _articlesEnabled.value = enabled
        if (enabled) {
            val articles = withContext(Dispatchers.IO) {
                runCatching { appContext.assets.open("StoryResources/contemporary_articles.json").bufferedReader().use { json.decodeFromString<List<StoryItem>>(it.readText()) } }.getOrDefault(emptyList())
            }
            _stories.value = (loaded + articles).shuffled()
        }
    }

    suspend fun storyText(story: StoryItem): String {
        return withContext(Dispatchers.IO) {
            runCatching {
                appContext.assets.open(story.storyTextAssetPath).use { input ->
                    val raw = input.bufferedReader().readText().trim()
                    if (raw.startsWith(story.title)) {
                        raw.removePrefix(story.title).trim()
                    } else {
                        raw
                    }
                }
            }.getOrDefault("")
        }
    }
}

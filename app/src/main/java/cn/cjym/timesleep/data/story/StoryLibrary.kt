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
import kotlinx.serialization.json.Json

class StoryLibrary(context: Context) {
    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }

    private val _stories = MutableStateFlow<List<StoryItem>>(emptyList())
    val stories: StateFlow<List<StoryItem>> = _stories.asStateFlow()

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

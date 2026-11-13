/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoryItem(
    val id: String,
    val index: Int,
    val directory: String = "",
    val title: String,
    val coverFile: String,
    val audioFile: String = "",
    val author: String? = null,
    val summary: String? = null,
    val textFile: String? = null,
    val duration: Double? = null,
    val isBundled: Boolean = false,
) {
    val isArticle: Boolean get() = index >= 1000
    val coverAssetPath: String get() = if (coverFile.startsWith("StoryResources/")) coverFile else "StoryResources/$directory/$coverFile"
    val storyTextAssetPath: String get() = textFile ?: "StoryResources/$directory/story.txt"
    val audioAssetPath: String get() = "StoryResources/$directory/$audioFile"
}

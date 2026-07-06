/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StoryItem(
    val id: String,
    val index: Int,
    val directory: String,
    val title: String,
    val coverFile: String,
    val audioFile: String,
    val duration: Double? = null,
    val isBundled: Boolean = false,
) {
    val coverAssetPath: String get() = "StoryResources/$directory/$coverFile"
    val storyTextAssetPath: String get() = "StoryResources/$directory/story.txt"
    val audioAssetPath: String get() = "StoryResources/$directory/$audioFile"
}

package cn.cjym.timesleep.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SoundScene(
    val id: String,
    val index: Int,
    val directory: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val coverFile: String,
    val audioFile: String,
    val duration: Double? = null,
) {
    val coverAssetPath: String get() = "SoundResources/$directory/$coverFile"
    val audioAssetPath: String get() = "SoundResources/$directory/$audioFile"
    val audioExtension: String get() = audioFile.substringAfterLast('.', "mp3")
    val isBundledAudio: Boolean get() = index <= 20
}

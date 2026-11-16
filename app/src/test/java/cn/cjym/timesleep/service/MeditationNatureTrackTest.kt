package cn.cjym.timesleep.service

import cn.cjym.timesleep.data.model.SoundScene
import org.junit.Assert.*
import org.junit.Test

class MeditationNatureTrackTest {
    private fun scene(index:Int) = SoundScene("sound-$index",index,"forest","Forest","","nature","cover.jpg","audio.mp3")
    @Test fun bundledNatureKeepsItsAudioAndCover() {
        val scene=scene(1)
        val track=MeditationLibrary.natureTrack(scene)
        assertEquals(scene.audioAssetPath,track.asset)
        assertEquals(scene.coverAssetPath,track.coverAsset)
        assertEquals(track,MeditationLibrary.find(track.id))
        assertFalse(track.requiresVIP)
    }
    @Test fun remoteNatureUsesSoundEndpointAndPreservesEntitlement() {
        val scene=scene(31)
        val track=MeditationLibrary.natureTrack(scene)
        assertEquals("",track.asset)
        assertTrue(track.downloadUrl.endsWith("/api/sounds/sound-31/download"))
        assertTrue(track.requiresVIP)
        assertTrue(track.subtitle.isNotBlank())
    }
}

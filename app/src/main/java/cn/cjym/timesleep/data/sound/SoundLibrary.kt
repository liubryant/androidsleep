/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.data.sound

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.cjym.timesleep.data.model.SoundScene
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.favoritesDataStore by preferencesDataStore(name = "sound_favorites")

class SoundLibrary(private val context: Context) {

    private val favoritesKey = stringSetPreferencesKey("sounds.favorites")

    private val _scenes = MutableStateFlow<List<SoundScene>>(emptyList())
    val scenes: StateFlow<List<SoundScene>> = _scenes.asStateFlow()

    val favorites: Flow<Set<String>> = context.favoritesDataStore.data.map { it[favoritesKey] ?: emptySet() }

    suspend fun load() {
        if (_scenes.value.isNotEmpty()) return
        runCatching {
            val json = context.assets.open("SoundResources/sounds_manifest.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<List<SoundScene>>(json).sortedBy { it.index }
        }.onSuccess { _scenes.value = it }
            .onFailure { _scenes.value = emptyList() }
    }

    suspend fun isFavorite(scene: SoundScene): Boolean {
        return favorites.first().contains(scene.id)
    }

    suspend fun toggleFavorite(scene: SoundScene) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (current.contains(scene.id)) current - scene.id else current + scene.id
        }
    }
}

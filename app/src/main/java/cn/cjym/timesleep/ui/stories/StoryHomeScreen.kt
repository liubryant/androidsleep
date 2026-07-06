/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.stories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AudioPlayerManager
import cn.cjym.timesleep.service.StoryPlayerManager
import cn.cjym.timesleep.ui.shared.EmptyState

@Composable
fun StoryHomeScreen(
    onOpenStory: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val library = app.storyLibrary
    val stories by library.stories.collectAsState()
    val storyPlaying by StoryPlayerManager.nowPlaying.collectAsState()
    val soundPlaying by AudioPlayerManager.nowPlaying.collectAsState()

    LaunchedEffect(Unit) {
        library.load()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (stories.isEmpty()) {
            EmptyState(
                title = "暂无故事资源",
                message = "请确认 StoryResources 已导入。",
                icon = Icons.Outlined.AutoStories,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (storyPlaying == null && soundPlaying == null) 16.dp else 104.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(stories, key = { it.id }) { story ->
                    StoryCard(
                        story = story,
                        modifier = Modifier.clickable { onOpenStory(story.id) },
                    )
                }
            }
        }
    }
}

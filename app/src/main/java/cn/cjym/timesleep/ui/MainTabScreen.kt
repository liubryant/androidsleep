/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.cjym.timesleep.service.AudioPlayerManager
import cn.cjym.timesleep.service.NowPlaying
import cn.cjym.timesleep.service.StoryNowPlaying
import cn.cjym.timesleep.service.StoryPlayerManager
import cn.cjym.timesleep.ui.profile.ProfileScreen
import cn.cjym.timesleep.ui.shared.AssetCoverImage
import cn.cjym.timesleep.ui.sleep.SleepHomeScreen
import cn.cjym.timesleep.ui.sounds.SoundHomeScreen
import cn.cjym.timesleep.ui.stories.StoryHomeScreen
import cn.cjym.timesleep.ui.stories.StoryPlayerScreen

private sealed class MainTab(val route: String, val label: String) {
    data object Sounds : MainTab("sounds", "声音")
    data object Sleep : MainTab("sleep", "睡眠")
    data object Stories : MainTab("stories", "故事")
    data object Profile : MainTab("profile", "我的")

    companion object {
        val all = listOf(Sounds, Sleep, Stories, Profile)
    }
}

private object StoryRoutes {
    const val Detail = "story/{storyId}"
    fun detail(storyId: String) = "story/$storyId"
}

@Composable
fun MainTabScreen() {
    val navController = rememberNavController()
    val nowPlaying by AudioPlayerManager.nowPlaying.collectAsState()
    val storyPlaying by StoryPlayerManager.nowPlaying.collectAsState()

    LaunchedEffect(nowPlaying?.scene?.id) {
        if (nowPlaying != null && storyPlaying != null) {
            StoryPlayerManager.stop()
        }
    }

    Scaffold(
        bottomBar = { MainBottomBar(navController) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = MainTab.Sounds.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                composable(MainTab.Sounds.route) { SoundHomeScreen() }
                composable(MainTab.Sleep.route) { SleepHomeScreen() }
                composable(MainTab.Stories.route) {
                    StoryHomeScreen(onOpenStory = { storyId ->
                        navController.navigate(StoryRoutes.detail(storyId))
                    })
                }
                composable(StoryRoutes.Detail) { backStackEntry ->
                    val storyId = backStackEntry.arguments?.getString("storyId").orEmpty()
                    StoryPlayerScreen(
                        storyId = storyId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(MainTab.Profile.route) { ProfileScreen() }
            }

            AnimatedVisibility(
                visible = storyPlaying != null || nowPlaying != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp)
                    .padding(horizontal = 16.dp),
            ) {
                if (storyPlaying != null) {
                    storyPlaying?.let { playing -> GlobalStoryMiniPlayer(playing = playing) }
                } else {
                    nowPlaying?.let { playing -> GlobalMiniPlayer(playing = playing) }
                }
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
    ) {
        MainTab.all.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true ||
                (tab == MainTab.Stories && currentRoute == StoryRoutes.Detail)
            val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(MainTab.Sounds.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            MainTab.Sounds -> Icons.Filled.GraphicEq
                            MainTab.Sleep -> Icons.Filled.Bedtime
                            MainTab.Stories -> Icons.Filled.Book
                            MainTab.Profile -> Icons.Filled.AccountCircle
                        },
                        contentDescription = tab.label,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = color,
                    selectedTextColor = color,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                ),
            )
        }
    }
}

@Composable
private fun GlobalStoryMiniPlayer(playing: StoryNowPlaying) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            AssetCoverImage(
                assetPath = playing.story.coverAssetPath,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playing.story.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "睡前故事",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = {
                if (playing.isPlaying) {
                    StoryPlayerManager.pause()
                } else {
                    StoryPlayerManager.resume(playing.story, "")
                }
            }) {
                Icon(
                    imageVector = if (playing.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing.isPlaying) "暂停" else "播放",
                )
            }

            IconButton(onClick = { StoryPlayerManager.stop() }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "关闭",
                )
            }
        }
    }
}

@Composable
private fun GlobalMiniPlayer(playing: NowPlaying) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AssetCoverImage(
                        assetPath = playing.scene.coverAssetPath,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playing.scene.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = playing.scene.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        playing.sleepTimerText?.let { timerText ->
                            Text(
                                text = timerText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    IconButton(onClick = { AudioPlayerManager.toggle(playing.scene) }) {
                        Icon(
                            imageVector = if (playing.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing.isPlaying) "暂停" else "播放",
                        )
                    }

                    IconButton(onClick = { AudioPlayerManager.stop() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "关闭",
                        )
                    }
                }
            }
        }
    }
}

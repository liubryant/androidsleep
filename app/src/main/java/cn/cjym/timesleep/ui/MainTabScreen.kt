/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
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
import cn.cjym.timesleep.ui.wellness.*
import cn.cjym.timesleep.service.MindfulnessPlayer
import androidx.compose.foundation.selection.selectable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
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

internal enum class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Sounds("sounds", "声音", Icons.Filled.GraphicEq),
    Sleep("sleep", "睡眠", Icons.Filled.Bedtime),
    Stories("stories", "故事", Icons.Filled.Book),
    Profile("profile", "我的", Icons.Filled.AccountCircle);
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

    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val topLevel = route in listOf("sounds", "sleep", "stories", "profile")
    val meditation by MindfulnessPlayer.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? cn.cjym.timesleep.MainActivity
    LaunchedEffect(activity?.pendingRoute) {
        activity?.pendingRoute?.let { requested -> navController.navigate(requested) { launchSingleTop=true }; activity.consumeRoute() }
    }
    Scaffold(
        bottomBar = { if (topLevel) MainBottomBar(navController) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = MainTab.Sounds.route,
                // Replace pages immediately so outgoing content cannot bleed through the next tab.
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                composable(MainTab.Sounds.route) { SoundHomeScreen(navigate = { navController.navigate(it) }) }
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
                composable("login") { cn.cjym.timesleep.ui.profile.LoginScreen(onBack = { navController.popBackStack() }) }
                composable(MainTab.Profile.route) { ProfileScreen(onExplore = { navController.navigate(it) }) }
                composable("wellness") { WellnessHubScreen({ navController.popBackStack() }, { navController.navigate(it) }) }
                composable("assessment/{kind}") { AssessmentScreen(it.arguments?.getString("kind") ?: "mood", { navController.popBackStack() }, { navController.navigate(it) }) }
                composable("history") { WellnessHistoryScreen({ navController.popBackStack() }, { navController.navigate(it) }) }
                composable("record/{id}") { WellnessRecordScreen(it.arguments?.getString("id").orEmpty(), { navController.popBackStack() }) }
                composable("inspiration") { InspirationScreen({ navController.popBackStack() }, { navController.navigate(it) }) }
                composable("breathing") { BreathingScreen { navController.popBackStack() } }
                composable("meditations") { MeditationHomeScreen({ navController.popBackStack() }, { navController.navigate(it) }) }
                composable("meditation/{id}") { MeditationPlayerScreen(it.arguments?.getString("id").orEmpty(), { navController.popBackStack() }, { navController.navigate(it) }) }
                composable("voices") { VoiceCharactersScreen { navController.popBackStack() } }
                composable("health") { HealthInsightScreen { navController.popBackStack() } }
                composable("devices") { DevicesScreen({ navController.popBackStack() }, { navController.navigate(it) }) }
                composable("membership") { MembershipScreen { navController.popBackStack() } }
            }

            AnimatedVisibility(
                visible = (topLevel || route == "meditations") && (storyPlaying != null || nowPlaying != null || meditation != null),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 8.dp)
                    .padding(horizontal = 16.dp),
            ) {
                if (meditation != null) {
                    meditation?.let { playing ->
                        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .98f), modifier = Modifier.clickable { navController.navigate("meditation/${playing.track.id}") }) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                AssetCoverImage("SoundResources/Mindfulness/Covers/cover-%02d.jpg".format(playing.track.cover), Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(playing.track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(playing.remainingSeconds?.let { timeText(it.toFloat()) + " 后暂停" } ?: playing.track.subtitle, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { MindfulnessPlayer.toggle() }) { Icon(if (playing.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "播放或暂停") }
                                IconButton(onClick = { MindfulnessPlayer.stop() }) { Icon(Icons.Default.Close, "停止") }
                            }
                        }
                    }
                } else if (storyPlaying != null) {
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

    Row(
        Modifier.fillMaxWidth().background(Color(0xFF17181C))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(56.dp).padding(horizontal=12.dp,vertical=4.dp),
        horizontalArrangement=Arrangement.spacedBy(8.dp),
        verticalAlignment=Alignment.CenterVertically,
    ) {
        MainTab.entries.forEach {tab ->
            val selected=currentDestination?.hierarchy?.any {it.route==tab.route}==true || (tab==MainTab.Stories && currentRoute==StoryRoutes.Detail)
            val color by animateColorAsState(if(selected) Color(0xFF0A84FF) else Color(0xFF979AA3),label="tabTint")
            val background by animateColorAsState(if(selected) Color(0xFF333336) else Color.Transparent,label="tabBackground")
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp))
                    .background(background)
                    .selectable(selected=selected,role=Role.Tab,onClick={
                        navController.navigate(tab.route) {
                            popUpTo(MainTab.Sounds.route) {saveState=true}
                            launchSingleTop=true
                            restoreState=true
                        }
                    }),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(2.dp,Alignment.CenterVertically),
            ) {
                if(tab==MainTab.Stories) {
                    Icon(androidx.compose.ui.res.painterResource(cn.cjym.timesleep.R.drawable.ic_tab_story),null,Modifier.size(22.dp),tint=color)
                } else {
                    Icon(tab.icon,null,Modifier.size(22.dp),tint=color)
                }
                Text(tab.label,fontSize=10.sp,lineHeight=12.sp,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal,color=color)
            }
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            AssetCoverImage(
                assetPath = playing.story.coverAssetPath,
                modifier = Modifier
                    .size(36.dp)
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
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AssetCoverImage(
                        assetPath = playing.scene.coverAssetPath,
                        modifier = Modifier
                            .size(36.dp)
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
                            text = playing.sleepTimerText ?: playing.scene.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

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

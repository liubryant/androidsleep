/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.stories

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import cn.cjym.timesleep.ui.shared.IOSSlider
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.data.model.StoryItem
import cn.cjym.timesleep.service.StoryPlayerManager
import cn.cjym.timesleep.ui.shared.AssetCoverImage
import kotlin.math.roundToInt

@Composable
fun StoryPlayerScreen(
    storyId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val stories by app.storyLibrary.stories.collectAsState()
    var selectedStoryId by rememberSaveable(storyId) { mutableStateOf(storyId) }
    val story = stories.firstOrNull { it.id == selectedStoryId }
    var timerMenu by remember {mutableStateOf(false)}
    val timerMinutes by StoryPlayerManager.timerMinutes.collectAsState()
    val playbackPrefs=remember {context.getSharedPreferences("story_playback",0)}
    var randomOrder by rememberSaveable {mutableStateOf(playbackPrefs.getBoolean("random",false))}

    LaunchedEffect(Unit) {
        app.storyLibrary.load()
    }

    if (story == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("故事不存在")
        }
        return
    }

    var storyText by remember(story.id) { mutableStateOf("") }
    val nowPlaying by StoryPlayerManager.nowPlaying.collectAsState()
    val isCurrent = nowPlaying?.story?.id == story.id
    val isPlayingThis = isCurrent && nowPlaying?.isPlaying == true

    LaunchedEffect(story.id) {
        storyText = app.storyLibrary.storyText(story)
        val text = storyText.ifBlank { "暂无故事正文" }
        StoryPlayerManager.play(story, text)
    }

    LaunchedEffect(isPlayingThis,nowPlaying?.currentTime) {
        val playing=nowPlaying
        if(isCurrent && playing!=null && !playing.isPlaying && playing.duration>0 && playing.currentTime>=playing.duration && stories.size>1) {
            selectedStoryId=if(randomOrder) stories.filter {it.id!=story.id}.random().id else stories[(stories.indexOf(story)+1)%stories.size].id
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        AssetCoverImage(story.coverAssetPath,Modifier.matchParentSize().blur(28.dp))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color(0x6B081226),Color(0xA8141730),Color(0xD60A0F21)))))
        val compact = maxHeight < 600.dp
        val narrow = maxWidth < 350.dp
        val coverSize = minOf(190.dp, maxOf(108.dp, minOf(maxWidth * 0.52f, maxHeight * 0.25f)))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (compact) 2.dp else 8.dp),
        ) {
            cn.cjym.timesleep.ui.wellness.WellnessTopBar(story.title,onBack) {Box {IconButton(onClick={timerMenu=true}) {Icon(Icons.Default.Timer,"定时",tint=Color.White)};DropdownMenu(timerMenu,{timerMenu=false}) {listOf(15,30,60).forEach {m->DropdownMenuItem({Text("$m 分钟")},{StoryPlayerManager.setSleepTimer(m);timerMenu=false})};DropdownMenuItem({Text("关闭定时")},{StoryPlayerManager.setSleepTimer(null);timerMenu=false})}}}
            RotatingStoryCover(
                story = story,
                isPlaying = isPlayingThis,
                modifier = Modifier.size(coverSize),
            )

            StoryPlaybackText(
                text = storyText.ifBlank { "暂无故事正文" },
                currentTime = if (isCurrent) nowPlaying?.currentTime ?: 0.0 else 0.0,
                duration = if (isCurrent) nowPlaying?.duration ?: 0.0 else 0.0,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = if (narrow) 12.dp else 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp,Color.White.copy(alpha=.16f),RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha=.16f)),
            )

            StoryControls(
                story = story,
                storyText = storyText.ifBlank { "暂无故事正文" },
                isCurrent = isCurrent,
                isPlaying = isPlayingThis,
                currentTime = if (isCurrent) nowPlaying?.currentTime ?: 0.0 else 0.0,
                duration = if (isCurrent) nowPlaying?.duration ?: 0.0 else 0.0,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (narrow) 16.dp else 24.dp)
                    .padding(bottom = if (compact) 4.dp else 12.dp),
            )
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.padding(bottom=12.dp)) {
                listOf(false,true).forEach {random->
                    Surface(onClick={randomOrder=random;playbackPrefs.edit().putBoolean("random",random).apply()},shape=RoundedCornerShape(24.dp),color=Color.White.copy(alpha=if(randomOrder==random) .22f else .09f)) {
                        Row(Modifier.padding(horizontal=14.dp,vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {Icon(if(random) Icons.Default.Shuffle else Icons.Default.Repeat,null,Modifier.size(18.dp),tint=Color.White);Text(if(random) "随机播放" else "顺序播放",fontSize=14.sp,color=Color.White)}
                    }
                }
            }
        }


    }
}

@Composable
private fun RotatingStoryCover(
    story: StoryItem,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "story-cover")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "story-cover-rotation",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .graphicsLayer { rotationZ = if (isPlaying) rotation else 0f },
        ) {
            AssetCoverImage(
                assetPath = story.coverAssetPath,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                placeholderIcon = Icons.Outlined.AutoStories,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Transparent,
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.08f),
                            ),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun StoryPlaybackText(
    text: String,
    currentTime: Double,
    duration: Double,
    modifier: Modifier = Modifier,
) {
    val chunks = remember(text) { splitStoryText(text) }
    val activeIndex by remember(currentTime, duration, chunks.size) {
        derivedStateOf {
            if (duration <= 0 || chunks.isEmpty()) {
                0
            } else {
                val progress = (currentTime / duration).coerceIn(0.0, 0.999)
                (progress * chunks.size).toInt().coerceIn(0, chunks.lastIndex)
            }
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (chunks.isNotEmpty()) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        itemsIndexed(chunks) { index, chunk ->
            val highlighted = index == activeIndex || index == activeIndex + 1
            Text(
                text = chunk,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.15,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(7.dp))
                    .background(
                        if (highlighted) {
                            MaterialTheme.colorScheme.primary.copy(alpha = if (index == activeIndex) 0.18f else 0.10f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                    )
                    .padding(horizontal = if (highlighted) 8.dp else 0.dp, vertical = if (highlighted) 5.dp else 0.dp),
            )
        }
    }
}

@Composable
private fun StoryControls(
    story: StoryItem,
    storyText: String,
    isCurrent: Boolean,
    isPlaying: Boolean,
    currentTime: Double,
    duration: Double,
    modifier: Modifier = Modifier,
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val displayValue = if (isSeeking) seekValue.toDouble() else currentTime
    val maxValue = duration.coerceAtLeast(0.0).toFloat()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        IOSSlider(
            value = displayValue.coerceIn(0.0, duration.coerceAtLeast(0.0)).toFloat(),
            onValueChange = {
                isSeeking = true
                seekValue = it
            },
            onValueChangeFinished = {
                StoryPlayerManager.seek(seekValue.toDouble())
                isSeeking = false
            },
            valueRange = 0f..maxValue.coerceAtLeast(1f),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = timeText(displayValue),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = timeText(if (isCurrent) duration else 0.0),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={StoryPlayerManager.seek((currentTime-15).coerceAtLeast(0.0))}) {Text("↶ 15",fontSize=22.sp,color=Color.White)}
            FilledIconButton(onClick={StoryPlayerManager.toggle(story,storyText)},modifier=Modifier.size(72.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=Color.White,contentColor=Color.Black)) {Icon(if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,if(isPlaying) "暂停" else "播放",Modifier.size(30.dp))}
            TextButton(onClick={StoryPlayerManager.seek((currentTime+15).coerceAtMost(duration))}) {Text("15 ↷",fontSize=22.sp,color=Color.White)}
        }

    }
}

private fun splitStoryText(source: String): List<String> {
    val normalized = source.replace("\r\n", "\n").trim()
    if (normalized.isBlank()) return listOf("暂无故事正文")

    val separators = setOf('，', ',', '。', '！', '？', '!', '?', '；', ';', '\n')
    val result = mutableListOf<String>()
    val current = StringBuilder()
    normalized.forEach { char ->
        current.append(char)
        if (char in separators) {
            appendChunk(current, result)
        }
    }
    appendChunk(current, result)
    return result.ifEmpty { listOf(normalized) }
}

private fun appendChunk(current: StringBuilder, result: MutableList<String>) {
    val trimmed = current.toString().trim()
    if (trimmed.isNotEmpty()) result += trimmed
    current.clear()
}

private fun timeText(seconds: Double): String {
    if (!seconds.isFinite() || seconds < 0) return "00:00"
    val total = seconds.roundToInt()
    return "%02d:%02d".format(total / 60, total % 60)
}

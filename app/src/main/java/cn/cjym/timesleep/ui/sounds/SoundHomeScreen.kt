package cn.cjym.timesleep.ui.sounds

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.R
import cn.cjym.timesleep.TimeSleepApp
import cn.cjym.timesleep.service.AudioPlayerManager
import cn.cjym.timesleep.ui.shared.EmptyState
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun SoundHomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as TimeSleepApp
    val library = app.soundLibrary
    val scope = rememberCoroutineScope()

    val scenes by library.scenes.collectAsState()
    val favorites by library.favorites.collectAsState(initial = emptySet())
    val nowPlaying by AudioPlayerManager.nowPlaying.collectAsState()

    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(SoundCategory.RECOMMENDED) }
    var didAutoPlay by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        library.load()
    }

    val filteredScenes = remember(scenes, searchText) {
        val text = searchText.trim()
        if (text.isEmpty()) {
            scenes
        } else {
            scenes.filter { scene ->
                scene.title.contains(text, ignoreCase = true) ||
                    scene.subtitle.contains(text, ignoreCase = true) ||
                    scene.category.contains(text, ignoreCase = true)
            }
        }
    }

    val displayedScenes = remember(filteredScenes, selectedCategory, favorites) {
        selectedCategory.scenes(from = filteredScenes, favorites = favorites)
    }

    LaunchedEffect(scenes.size) {
        if (!didAutoPlay && nowPlaying == null && displayedScenes.isNotEmpty()) {
            didAutoPlay = true
            AudioPlayerManager.toggle(displayedScenes.first())
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        SoundTopBar(
            selectedCategory = selectedCategory,
            onSelectAll = {
                selectedCategory = SoundCategory.ALL
                searchText = ""
            },
            onSelectFavorites = { selectedCategory = SoundCategory.FAVORITES },
            onSetSleepTimer = { minutes -> AudioPlayerManager.setSleepTimer(minutes) },
            onCancelSleepTimer = { AudioPlayerManager.cancelSleepTimer() },
        )

        SoundSearchBar(searchText = searchText, onSearchTextChange = { searchText = it })

        SoundCategoryBar(selectedCategory = selectedCategory, onSelect = { selectedCategory = it })

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                scenes.isEmpty() -> EmptyState(
                    title = "暂无声音资源",
                    message = "请确认 SoundResources 已导入。",
                    icon = Icons.Filled.Search,
                    modifier = Modifier.align(Alignment.Center),
                )
                displayedScenes.isEmpty() -> EmptyState(
                    title = "没有匹配的声音",
                    message = selectedCategory.emptyMessage,
                    icon = Icons.Filled.Search,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> {
                    val bottomPadding = if (nowPlaying == null) 16.dp else 96.dp
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        displayedScenes.chunked(4).forEach { group ->
                            items(group, key = { it.id }) { scene ->
                                SoundCard(
                                    scene = scene,
                                    isFavorite = favorites.contains(scene.id),
                                    onToggleFavorite = {
                                        scope.launch { library.toggleFavorite(scene) }
                                    },
                                )
                            }
                            if (group.size == 4) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    DrawFeedAdCard()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundTopBar(
    selectedCategory: SoundCategory,
    onSelectAll: () -> Unit,
    onSelectFavorites: () -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 18.dp),
    ) {
        Image(
            painter = painterResource(id = R.drawable.launcher_icon),
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp)),
        )

        Text(
            text = greetingText(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        )

        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(imageVector = Icons.Filled.Tune, contentDescription = "筛选")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("全部") }, onClick = { menuExpanded = false; onSelectAll() })
                DropdownMenuItem(text = { Text("收藏") }, onClick = { menuExpanded = false; onSelectFavorites() })
                HorizontalDivider()
                DropdownMenuItem(text = { Text("15 分钟后停止") }, onClick = { menuExpanded = false; onSetSleepTimer(15) })
                DropdownMenuItem(text = { Text("30 分钟后停止") }, onClick = { menuExpanded = false; onSetSleepTimer(30) })
                DropdownMenuItem(text = { Text("60 分钟后停止") }, onClick = { menuExpanded = false; onSetSleepTimer(60) })
                DropdownMenuItem(text = { Text("取消定时") }, onClick = { menuExpanded = false; onCancelSleepTimer() })
            }
        }
    }
}

@Composable
private fun SoundSearchBar(searchText: String, onSearchTextChange: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp)
            .height(40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )

            Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                if (searchText.isEmpty()) {
                    Text(
                        text = "搜索雨声、森林、海浪",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = onSearchTextChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (searchText.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = "清除",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onSearchTextChange("") },
                )
            }
        }
    }
}

@Composable
private fun SoundCategoryBar(selectedCategory: SoundCategory, onSelect: (SoundCategory) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SoundCategory.entries.forEach { category ->
            val selected = category == selectedCategory
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .clickable { onSelect(category) },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = category.title,
                        style = if (selected) MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) else MaterialTheme.typography.bodyMedium,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

private fun greetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..7 -> "早上好"
        in 8..11 -> "上午好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..20 -> "傍晚好"
        in 21..23 -> "晚上好"
        else -> "夜深了"
    }
    return "$greeting，时光睡眠！"
}

package cn.cjym.timesleep.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cn.cjym.timesleep.TimeSleepApp

@Composable fun StoryHomeScreen(onOpenStory:(String)->Unit,modifier:Modifier=Modifier) {
    val library=(LocalContext.current.applicationContext as TimeSleepApp).storyLibrary
    val stories by library.stories.collectAsState()
    val favorites by library.favorites.collectAsState()
    val articles by library.articlesEnabled.collectAsState()
    var search by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { library.load() }
    val filtered=remember(stories,favorites,search) { stories.filter { search.isBlank() || "${it.title} ${it.author.orEmpty()} ${it.summary.orEmpty()}".contains(search.trim(),true) }.sortedByDescending { it.id in favorites } }
    Column(modifier.fillMaxSize()) {
        Text("故事",Modifier.padding(16.dp),fontSize=20.sp,fontWeight=FontWeight.Bold)
        LazyVerticalGrid(columns=GridCells.Fixed(2),contentPadding=PaddingValues(start=16.dp,end=16.dp,bottom=100.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item(span={GridItemSpan(maxLineSpan)}) { BasicTextField(search,{search=it},Modifier.fillMaxWidth().height(42.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).background(Color(0xFF1C1C1E)),singleLine=true,textStyle=MaterialTheme.typography.bodyMedium.copy(fontSize=15.sp,color=Color.White),cursorBrush=androidx.compose.ui.graphics.SolidColor(Color.White),decorationBox={inner->Row(Modifier.fillMaxSize().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {Icon(Icons.Default.Search,null,Modifier.size(20.dp),tint=Color.Gray);Box(Modifier.weight(1f)){if(search.isEmpty()) Text("搜索故事、文章",fontSize=15.sp,color=Color.Gray);inner()};if(search.isNotEmpty()) IconButton(onClick={search=""},modifier=Modifier.size(24.dp)){Icon(Icons.Default.Cancel,"清除",Modifier.size(18.dp),tint=Color.Gray)}}}) }
            item(span={GridItemSpan(maxLineSpan)}) { Row(Modifier.fillMaxWidth()) {Text(if(articles) "故事推荐" else "睡前故事",Modifier.weight(1f),fontSize=20.sp,fontWeight=FontWeight.SemiBold);Text("${filtered.size}篇",style=MaterialTheme.typography.bodySmall)} }
            if(filtered.isEmpty()) item(span={GridItemSpan(maxLineSpan)}) { Text(if(stories.isEmpty()) "正在加载故事…" else "未找到相关内容，换个标题或作者试试。") }
            items(filtered,key={it.id}) { story -> Box {
                StoryCard(story,Modifier.clickable {onOpenStory(story.id)})
                Surface(shape=CircleShape,color=Color.Black.copy(alpha=.4f),modifier=Modifier.align(Alignment.BottomEnd).padding(7.dp)) {
                    IconButton(onClick={library.toggleFavorite(story.id)},modifier=Modifier.size(32.dp)) {Icon(if(story.id in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,if(story.id in favorites) "取消收藏" else "收藏",Modifier.size(18.dp),tint=if(story.id in favorites) Color.Red else Color.White)}
                }
            } }
        }
    }
}

/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.ui.sounds

import cn.cjym.timesleep.data.model.SoundScene

enum class SoundCategory(val title: String, val emptyMessage: String) {
    RECOMMENDED("推荐", "换个关键词或查看全部声音。"),
    FAVORITES("收藏", "收藏喜欢的声音后会显示在这里。"),
    ALL("全部", "换个关键词或查看全部声音。"),
    MEDITATION("冥想", "换个关键词或查看全部声音。"),
    SLEEP("助眠", "换个关键词或查看全部声音。"),
    NATURE("自然", "换个关键词或查看全部声音。"),
    WHISPER("耳畔", "换个关键词或查看全部声音。"),
    WHITE_NOISE("白噪音", "换个关键词或查看全部声音。"),
    DREAM("梦境", "换个关键词或查看全部声音。"),
    MINDFULNESS("正念", "换个关键词或查看全部声音。");

    fun scenes(from: List<SoundScene>, favorites: Set<String>): List<SoundScene> {
        val scenes = from
        return when (this) {
            RECOMMENDED -> scenes.filter { it.index <= 20 }
            FAVORITES -> scenes.filter { favorites.contains(it.id) }
            ALL -> scenes
            MEDITATION -> limitedMatches(scenes, listOf("冥想", "声音浴", "钵", "寺", "钟", "光蕴", "七弦", "八音盒", "呼吸", "静"))
            SLEEP -> scenes.filter { it.category == "sleep" }.take(30)
            NATURE -> limitedMatches(scenes, listOf("雨", "风", "海", "山", "林", "森林", "竹林", "溪", "泉", "河", "湖", "岛", "星", "月", "雪", "虫", "鸟"))
            WHISPER -> limitedMatches(scenes, listOf("耳", "低语", "风铃", "键盘", "铅笔", "磨砚", "手谈", "切菜", "打字机", "心跳", "静电"))
            WHITE_NOISE -> scenes.filter { it.category == "white_noise" }.take(30)
            DREAM -> limitedMatches(scenes, listOf("梦", "夜", "月", "星", "幻", "蓝色", "浮空", "云", "微光", "深睡", "睡吧"))
            MINDFULNESS -> limitedMatches(scenes, listOf("正念", "冥想", "钵", "声音浴", "寺庙", "山泉", "竹林", "远山", "须臾", "柔软", "静"))
        }
    }

    private fun limitedMatches(scenes: List<SoundScene>, keywords: List<String>): List<SoundScene> {
        return scenes.filter { scene ->
            val text = "${scene.title} ${scene.subtitle} ${scene.category}"
            keywords.any { text.contains(it, ignoreCase = true) }
        }.take(30)
    }
}

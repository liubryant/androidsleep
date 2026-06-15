package cn.cjym.timesleep.service

import android.content.Context
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

/**
 * 缓存与临时文件管理，对应 iOS `CacheService`：统计/清理声音下载缓存、
 * 系统临时文件与睡眠录音文件。
 */
object CacheManager {

    fun cacheSize(context: Context): Long {
        return folderSize(context.cacheDir) + folderSize(SleepSessionStore.recordingsDirectory(context))
    }

    fun clearCache(context: Context) {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun formattedSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.size)
        val value = bytes / 1024.0.pow(exponent)
        return "%.1f %s".format(value, units[exponent - 1])
    }

    private fun folderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles()?.sumOf { folderSize(it) } ?: 0L
    }
}

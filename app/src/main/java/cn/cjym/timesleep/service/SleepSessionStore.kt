package cn.cjym.timesleep.service

import android.content.Context
import cn.cjym.timesleep.data.model.SleepSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 睡眠会话的本地 JSON 持久化，与 iOS `SleepSessionStore` 对应：
 * 会话列表存储在 `filesDir/SleepData/SleepSessions.json`，
 * 录音文件存储在 `filesDir/SleepData/Recordings/`。
 */
object SleepSessionStore {
    private const val fileName = "SleepSessions.json"
    private const val maxSessionCount = 90

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun loadSessions(context: Context): List<SleepSession> {
        val file = storeFile(context)
        if (!file.exists()) return emptyList()

        return try {
            json.decodeFromString<List<SleepSession>>(file.readText())
                .sortedByDescending { it.startTime }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSessions(context: Context, sessions: List<SleepSession>) {
        val normalized = sessions.sortedByDescending { it.startTime }.take(maxSessionCount)
        try {
            storeDirectory(context).mkdirs()
            storeFile(context).writeText(json.encodeToString(normalized))
        } catch (e: Exception) {
            // 忽略写入失败，下次会话结束时会重试。
        }
    }

    fun upsert(session: SleepSession, into: List<SleepSession>): List<SleepSession> {
        val result = into.filterNot { it.id == session.id }.toMutableList()
        result.add(0, session)
        result.sortByDescending { it.startTime }
        return result.take(maxSessionCount)
    }

    /** 从会话列表中移除指定会话（例如睡眠时长不足，不生成报告时）。 */
    fun remove(session: SleepSession, from: List<SleepSession>): List<SleepSession> =
        from.filterNot { it.id == session.id }

    /** 删除指定的录音文件。 */
    fun deleteRecording(context: Context, fileName: String) {
        File(recordingDirectory(context), fileName).delete()
    }

    fun recordingFile(context: Context, fileName: String): File = File(recordingDirectory(context), fileName)

    fun makeRecordingFile(context: Context, sessionId: String): File {
        recordingDirectory(context).mkdirs()
        return File(recordingDirectory(context), "$sessionId.wav")
    }

    fun clearRecordings(context: Context) {
        recordingDirectory(context).listFiles()?.forEach { it.delete() }
    }

    fun recordingsDirectory(context: Context): File = recordingDirectory(context)

    private fun storeDirectory(context: Context): File = File(context.filesDir, "SleepData")

    private fun storeFile(context: Context): File = File(storeDirectory(context), fileName)

    private fun recordingDirectory(context: Context): File = File(storeDirectory(context), "Recordings")
}

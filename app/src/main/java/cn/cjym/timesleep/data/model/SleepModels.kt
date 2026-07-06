/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Serializable
enum class SleepEventType {
    snore, cough, sleepTalk, bruxism, noise, heavyBreathing, nasalCongestion, fart, breathHolding;

    val title: String
        get() = when (this) {
            snore -> "打鼾"
            cough -> "咳嗽"
            sleepTalk -> "说梦话"
            bruxism -> "磨牙"
            noise -> "环境噪音"
            heavyBreathing -> "大口呼吸"
            nasalCongestion -> "鼻塞"
            fart -> "放屁"
            breathHolding -> "憋气"
        }
}

@Serializable
data class SleepEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: SleepEventType,
    /** 事件起始时间（epoch 毫秒）。 */
    val startTime: Long,
    /** 事件结束时间（epoch 毫秒）。 */
    val endTime: Long,
    val confidence: Double,
    val peakDecibel: Double,
)

@Serializable
data class NoiseSample(
    val id: String = UUID.randomUUID().toString(),
    /** 采样时间（epoch 毫秒）。 */
    val time: Long,
    val decibel: Double,
)

@Serializable
data class SleepSession(
    val id: String = UUID.randomUUID().toString(),
    /** 会话起始时间（epoch 毫秒）。 */
    val startTime: Long = System.currentTimeMillis(),
    /** 会话结束时间（epoch 毫秒），监测中为 null。 */
    val endTime: Long? = null,
    val events: MutableList<SleepEvent> = mutableListOf(),
    val noiseSamples: MutableList<NoiseSample> = mutableListOf(),
    val audioFileName: String? = null,
) {
    /** 会话时长（秒），监测中以当前时间计算。 */
    val duration: Double
        get() = ((endTime ?: System.currentTimeMillis()) - startTime) / 1000.0

    val averageNoise: Double
        get() = if (noiseSamples.isEmpty()) 0.0 else noiseSamples.sumOf { it.decibel } / noiseSamples.size

    val maxNoise: Double
        get() = noiseSamples.maxOfOrNull { it.decibel } ?: 0.0

    val sleepScore: Int
        get() {
            val durationHours = duration / 3_600.0
            val durationPenalty = when {
                durationHours <= 0 -> 35.0
                durationHours < 6 -> (6 - durationHours) * 4
                durationHours > 9 -> min((durationHours - 9) * 2, 10.0)
                else -> 0.0
            }

            val noisePenalty = min(max(averageNoise - 38, 0.0) * 0.7, 22.0)
            val eventPenalty = min(events.size * 1.8, 28.0)
            val severeEventCount = eventCount(SleepEventType.snore) + eventCount(SleepEventType.bruxism) +
                eventCount(SleepEventType.heavyBreathing) + eventCount(SleepEventType.breathHolding) * 2
            val severeEventPenalty = min(severeEventCount * 2.2, 18.0)
            val score = 100 - durationPenalty - noisePenalty - eventPenalty - severeEventPenalty
            return min(100, max(60, score.roundToInt()))
        }

    /** 睡眠分布：将本次睡眠按声音特征划分为深睡/浅睡/做梦/觉醒区间。 */
    val sleepDistribution: SleepDistribution
        get() = SleepStageAnalyzer.analyze(this)

    /** 睡眠效率 = 实际睡眠时长 / 在床时长，百分比。 */
    val sleepEfficiency: Double
        get() {
            val distribution = sleepDistribution
            if (distribution.totalDuration <= 0) return 0.0
            return min(100.0, max(0.0, distribution.sleepDuration / distribution.totalDuration * 100))
        }

    /** 睡眠年龄：综合睡眠评分、深睡比例、睡眠效率和呼吸/磨牙事件估算的参考年龄。 */
    val sleepAge: Int
        get() {
            val distribution = sleepDistribution
            val sleepDuration = distribution.sleepDuration
            val deepRatio = if (sleepDuration > 0) distribution.duration(SleepStage.deep) / sleepDuration else 0.0

            var age = 28.0
            age += (75 - sleepScore) * 0.35
            age += max(0.0, 0.22 - deepRatio) * 160
            age += max(0.0, 85 - sleepEfficiency) * 0.3
            val disruptiveEvents = eventCount(SleepEventType.snore) + eventCount(SleepEventType.bruxism) +
                eventCount(SleepEventType.heavyBreathing) + eventCount(SleepEventType.breathHolding)
            age += disruptiveEvents * 0.6

            return min(60, max(16, age.roundToInt()))
        }

    fun eventCount(type: SleepEventType): Int = events.count { it.type == type }

    fun eventDuration(type: SleepEventType): Double = events
        .filter { it.type == type }
        .sumOf { max(0.0, (it.endTime - it.startTime) / 1000.0) }
}

enum class SleepStage {
    deep, light, rem, awake;

    val title: String
        get() = when (this) {
            deep -> "深度睡眠"
            light -> "浅睡眠"
            rem -> "做梦"
            awake -> "睡中觉醒"
        }
}

data class SleepStageSegment(
    val stage: SleepStage,
    val startTime: Long,
    val endTime: Long,
) {
    val duration: Double
        get() = max(0.0, (endTime - startTime) / 1000.0)
}

data class SleepDistribution(
    val segments: List<SleepStageSegment>,
    val fallAsleepTime: Long?,
    val wakeTime: Long?,
) {
    val totalDuration: Double
        get() = segments.sumOf { it.duration }

    /** 总睡眠时长（不含睡中觉醒时段）。 */
    val sleepDuration: Double
        get() = totalDuration - duration(SleepStage.awake)

    fun duration(stage: SleepStage): Double = segments.filter { it.stage == stage }.sumOf { it.duration }

    fun percentage(stage: SleepStage): Double {
        if (totalDuration <= 0) return 0.0
        return duration(stage) / totalDuration * 100
    }
}

/**
 * 基于环境噪音水平与识别事件，将一次睡眠会话划分为深睡/浅睡/做梦/觉醒区间。
 * 这是一种启发式估算，用于在没有专用生理传感器的情况下给出可解释的睡眠分布参考。
 */
object SleepStageAnalyzer {
    private const val windowSizeMs = 5 * 60 * 1000L
    private val disruptiveTypes = setOf(
        SleepEventType.snore, SleepEventType.bruxism, SleepEventType.cough, SleepEventType.heavyBreathing,
        SleepEventType.nasalCongestion, SleepEventType.fart, SleepEventType.noise, SleepEventType.breathHolding,
    )

    fun analyze(session: SleepSession): SleepDistribution {
        val endTime = session.endTime
        if (endTime == null || endTime <= session.startTime) {
            return SleepDistribution(emptyList(), null, null)
        }

        val baselineNoise = session.averageNoise
        val segments = mutableListOf<SleepStageSegment>()
        var cursor = session.startTime

        while (cursor < endTime) {
            val windowEnd = min(cursor + windowSizeMs, endTime)
            val stage = stageFor(session, cursor, windowEnd, baselineNoise)

            val last = segments.lastOrNull()
            if (last != null && last.stage == stage) {
                segments[segments.size - 1] = SleepStageSegment(stage, last.startTime, windowEnd)
            } else {
                segments.add(SleepStageSegment(stage, cursor, windowEnd))
            }

            cursor = windowEnd
        }

        var fallAsleepTime = session.startTime
        val first = segments.firstOrNull()
        if (first != null && first.stage == SleepStage.awake) {
            fallAsleepTime = first.endTime
        }

        return SleepDistribution(segments, fallAsleepTime, endTime)
    }

    private fun stageFor(session: SleepSession, start: Long, end: Long, baselineNoise: Double): SleepStage {
        val samples = session.noiseSamples.filter { it.time >= start && it.time < end }
        val averageDecibel = if (samples.isEmpty()) {
            baselineNoise
        } else {
            samples.sumOf { it.decibel } / samples.size
        }

        val events = session.events.filter { it.startTime < end && it.endTime > start }
        val disruptiveCount = events.count { it.type in disruptiveTypes }
        val sleepTalkCount = events.count { it.type == SleepEventType.sleepTalk }

        return when {
            averageDecibel > baselineNoise + 12 || disruptiveCount >= 3 -> SleepStage.awake
            sleepTalkCount >= 1 || (averageDecibel > baselineNoise + 4 && disruptiveCount >= 1) -> SleepStage.rem
            averageDecibel < baselineNoise - 3 && disruptiveCount == 0 -> SleepStage.deep
            else -> SleepStage.light
        }
    }
}

enum class SleepTrendRange(val title: String, val dayCount: Int) {
    week("周", 7),
    month("月", 30),
}

data class SleepTrendPoint(
    /** 当天起始时刻（epoch 毫秒）。 */
    val date: Long,
    val score: Double,
    val durationHours: Double,
    val eventCount: Int,
    val snoreCount: Int,
    val bruxismCount: Int,
    val averageNoise: Double,
)

/**
 * 当用户还没有任何睡眠记录时，用于展示的示例「优秀」睡眠报告。
 * 数据经过设计，呈现 8 小时睡眠、低噪音环境与较少干扰事件下的详细分析效果，
 * 对应 iOS `SleepSession.sample`。
 */
object SampleSleepSession {
    val instance: SleepSession
        get() {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 8L * 3_600 * 1_000

            val noiseSamples = (0 until 480).map { minute ->
                val t = minute.toDouble()
                val decibel = 31 +
                    5 * sin(2 * Math.PI * t / 90) +
                    1.5 * sin(2 * Math.PI * t / 23)
                NoiseSample(time = startTime + minute * 60_000L, decibel = decibel)
            }.toMutableList()

            fun eventTime(minute: Double) = startTime + (minute * 60_000).toLong()

            val events = mutableListOf(
                SleepEvent(type = SleepEventType.cough, startTime = eventTime(46.0), endTime = eventTime(46.0) + 2_000, confidence = 0.86, peakDecibel = 41.0),
                SleepEvent(type = SleepEventType.snore, startTime = eventTime(132.0), endTime = eventTime(132.0) + 6_000, confidence = 0.78, peakDecibel = 38.0),
                SleepEvent(type = SleepEventType.sleepTalk, startTime = eventTime(214.0), endTime = eventTime(214.0) + 4_000, confidence = 0.81, peakDecibel = 40.0),
                SleepEvent(type = SleepEventType.sleepTalk, startTime = eventTime(362.0), endTime = eventTime(362.0) + 3_000, confidence = 0.83, peakDecibel = 39.0),
            )

            return SleepSession(
                startTime = startTime,
                endTime = endTime,
                events = events,
                noiseSamples = noiseSamples,
                audioFileName = null,
            )
        }
}

object SleepTrendCalculator {
    fun points(sessions: List<SleepSession>, range: SleepTrendRange): List<SleepTrendPoint> {
        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val startDate = today.minusDays((range.dayCount - 1).toLong())

        val sessionsByDay = sessions.groupBy { session ->
            Instant.ofEpochMilli(session.startTime).atZone(zone).toLocalDate()
        }

        return (0 until range.dayCount).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dateMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val daySessions = sessionsByDay[date] ?: emptyList()

            if (daySessions.isEmpty()) {
                SleepTrendPoint(
                    date = dateMillis,
                    score = 0.0,
                    durationHours = 0.0,
                    eventCount = 0,
                    snoreCount = 0,
                    bruxismCount = 0,
                    averageNoise = 0.0,
                )
            } else {
                val totalDuration = daySessions.sumOf { it.duration }
                val totalEvents = daySessions.sumOf { it.events.size }
                val totalSnore = daySessions.sumOf { it.eventCount(SleepEventType.snore) }
                val totalBruxism = daySessions.sumOf { it.eventCount(SleepEventType.bruxism) }
                val averageScore = daySessions.sumOf { it.sleepScore }.toDouble() / daySessions.size
                val averageNoise = daySessions.sumOf { it.averageNoise } / daySessions.size

                SleepTrendPoint(
                    date = dateMillis,
                    score = averageScore,
                    durationHours = totalDuration / 3_600.0,
                    eventCount = totalEvents,
                    snoreCount = totalSnore,
                    bruxismCount = totalBruxism,
                    averageNoise = averageNoise,
                )
            }
        }
    }
}

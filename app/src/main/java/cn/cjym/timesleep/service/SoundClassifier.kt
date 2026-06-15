package cn.cjym.timesleep.service

import cn.cjym.timesleep.data.model.SleepEventType
import kotlin.math.min

data class SoundClassification(val type: SleepEventType, val confidence: Double)

interface SoundClassifying {
    fun classify(features: AudioFeatures): SoundClassification?
}

/**
 * 基于音频特征数值阈值的睡眠声音事件分类器，与 iOS
 * `FeatureBasedSleepSoundClassifier` 的判定规则与阈值 1:1 对应。
 */
class FeatureBasedSleepSoundClassifier : SoundClassifying {
    override fun classify(features: AudioFeatures): SoundClassification? {
        if (features.estimatedDecibel <= 36) return null

        if (features.estimatedDecibel > 68) {
            return SoundClassification(SleepEventType.noise, 0.82)
        }

        val isLowFrequencyDominant = features.lowBandRatio > 0.5f
        val isTonal = features.spectralFlatness < 0.3f
        val isBroadband = features.spectralFlatness > 0.35f

        // 放屁：低频为主、接近噪声的短促爆发音（峰值远高于均方根）。
        if (isLowFrequencyDominant && isBroadband && features.peak > 0.1f &&
            features.peak > features.rms * 6 && features.zeroCrossingRate < 0.045f
        ) {
            val confidence = min(0.85, 0.55 + features.peak.toDouble())
            return SoundClassification(SleepEventType.fart, confidence)
        }

        // 大口呼吸/打鼾：低频为主、音调性强的持续音，按响度区分严重程度。
        if (isLowFrequencyDominant && isTonal && features.zeroCrossingRate < 0.08f) {
            if (features.rms > 0.045f) {
                val confidence = min(0.92, 0.6 + features.rms * 3.0)
                return SoundClassification(SleepEventType.heavyBreathing, confidence)
            }
            if (features.rms > 0.018f) {
                val confidence = min(0.92, 0.64 + features.rms * 4.0)
                return SoundClassification(SleepEventType.snore, confidence)
            }
        }

        // 咳嗽：中高频为主的宽频带爆发音，峰值突出。
        if (features.peak > 0.2f && features.rms > 0.02f && isBroadband &&
            features.midBandRatio + features.highBandRatio > 0.5f
        ) {
            val confidence = min(0.88, 0.62 + features.peak.toDouble())
            return SoundClassification(SleepEventType.cough, confidence)
        }

        // 鼻塞：中频窄带哨鸣音，音调性强、强度适中、没有咳嗽那样的尖峰冲击。
        if (features.midBandRatio > 0.45f && isTonal &&
            features.rms > 0.012f && features.rms < 0.028f && features.peak < 0.15f
        ) {
            val confidence = min(0.8, 0.55 + features.rms * 5.0)
            return SoundClassification(SleepEventType.nasalCongestion, confidence)
        }

        // 磨牙：高频能量占比高、过零率高的摩擦音。
        if (features.highBandRatio > 0.4f && features.zeroCrossingRate > 0.08f && features.peak > 0.08f) {
            val confidence = min(0.86, 0.58 + features.zeroCrossingRate * 2.0)
            return SoundClassification(SleepEventType.bruxism, confidence)
        }

        // 说梦话：中高频混合、有一定能量和过零率，但不像磨牙那样尖锐。
        if (features.midBandRatio + features.highBandRatio > 0.4f &&
            features.rms > 0.012f && features.zeroCrossingRate > 0.035f
        ) {
            return SoundClassification(SleepEventType.sleepTalk, 0.64)
        }

        return null
    }
}

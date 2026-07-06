/**
 * Author: liuzheng <bryant_liu24@126.com>
 */

package cn.cjym.timesleep.service

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 单帧音频特征，与 iOS `AudioFeatureExtractor.AudioFeatures` 字段一一对应。
 */
data class AudioFeatures(
    val rms: Float,
    val peak: Float,
    val estimatedDecibel: Double,
    val zeroCrossingRate: Float,
    /** 频谱质心（Hz），频率越高表示声音越尖锐/明亮。 */
    val spectralCentroid: Float,
    /** 500Hz 以下能量占总能量的比例。 */
    val lowBandRatio: Float,
    /** 500Hz ~ 2000Hz 能量占总能量的比例。 */
    val midBandRatio: Float,
    /** 2000Hz 以上能量占总能量的比例。 */
    val highBandRatio: Float,
    /** 频谱平坦度（几何均值 / 算术均值），越接近 0 越偏音调性，越接近 1 越偏噪声。 */
    val spectralFlatness: Float,
)

/**
 * 纯 Kotlin 实现的音频特征提取，对应 iOS `Services/AudioFeatureExtractor.swift`
 * 的 vDSP 实现：RMS/峰值/分贝/过零率，以及基于 Radix-2 FFT 的频谱质心、
 * 低/中/高频带能量占比与频谱平坦度。
 */
object AudioFeatureExtractor {
    private const val lowBandCutoff = 500f
    private const val highBandCutoff = 2_000f

    /** FFT 长度上限 2^12 = 4096，覆盖典型 4096 采样的输入缓冲区。 */
    private const val maxFFTLength = 4_096

    fun features(samples: FloatArray, sampleRate: Float): AudioFeatures? {
        val count = samples.size
        if (count == 0) return null

        var sumSquares = 0.0
        var peak = 0f
        for (sample in samples) {
            sumSquares += (sample * sample).toDouble()
            val magnitude = abs(sample)
            if (magnitude > peak) peak = magnitude
        }
        val rms = sqrt(sumSquares / count).toFloat()

        val db = max(-80.0, 20 * log10(max(rms.toDouble(), 0.000_001)) + 90)
        val zeroCrossingRate = estimateZeroCrossingRate(samples)
        val spectrum = analyzeSpectrum(samples, sampleRate)

        return AudioFeatures(
            rms = rms,
            peak = peak,
            estimatedDecibel = db,
            zeroCrossingRate = zeroCrossingRate,
            spectralCentroid = spectrum.centroid,
            lowBandRatio = spectrum.lowBandRatio,
            midBandRatio = spectrum.midBandRatio,
            highBandRatio = spectrum.highBandRatio,
            spectralFlatness = spectrum.flatness,
        )
    }

    private data class SpectrumFeatures(
        val centroid: Float,
        val lowBandRatio: Float,
        val midBandRatio: Float,
        val highBandRatio: Float,
        val flatness: Float,
    )

    /**
     * 对采样进行 Hann 窗 + 实数 FFT，计算幅度谱（功率），并据此得到频谱质心、
     * 低/中/高频带能量占比与频谱平坦度。
     */
    private fun analyzeSpectrum(samples: FloatArray, sampleRate: Float): SpectrumFeatures {
        val count = samples.size
        val fftLen = fftLength(count)
        val half = fftLen / 2
        val usable = min(count, fftLen)

        val re = DoubleArray(fftLen)
        val im = DoubleArray(fftLen)
        for (i in 0 until usable) {
            val window = if (usable == 1) 1.0 else 0.5 * (1 - cos(2 * PI * i / (usable - 1)))
            re[i] = samples[i] * window
        }

        fft(re, im)

        val binWidth = sampleRate / fftLen
        var weightedSum = 0.0
        var totalMagnitude = 0.0
        var lowSum = 0.0
        var midSum = 0.0
        var highSum = 0.0
        var logSum = 0.0

        for (index in 0 until half) {
            val magnitude = re[index] * re[index] + im[index] * im[index]
            val frequency = index * binWidth
            weightedSum += frequency * magnitude
            totalMagnitude += magnitude
            logSum += ln(magnitude + 1e-9)

            when {
                frequency < lowBandCutoff -> lowSum += magnitude
                frequency < highBandCutoff -> midSum += magnitude
                else -> highSum += magnitude
            }
        }

        if (totalMagnitude <= 0) {
            return SpectrumFeatures(0f, 0f, 0f, 0f, 0f)
        }

        val meanMagnitude = totalMagnitude / half
        val geometricMean = exp(logSum / half)

        return SpectrumFeatures(
            centroid = (weightedSum / totalMagnitude).toFloat(),
            lowBandRatio = (lowSum / totalMagnitude).toFloat(),
            midBandRatio = (midSum / totalMagnitude).toFloat(),
            highBandRatio = (highSum / totalMagnitude).toFloat(),
            flatness = if (meanMagnitude > 0) (geometricMean / meanMagnitude).toFloat() else 0f,
        )
    }

    /** 返回不超过 `count` 且不超过 FFT 上限的最大 2 的幂，用作 FFT 长度。 */
    private fun fftLength(count: Int): Int {
        var length = 64
        while (length * 2 <= count && length < maxFFTLength) {
            length *= 2
        }
        return length
    }

    private fun estimateZeroCrossingRate(samples: FloatArray): Float {
        val count = samples.size
        if (count <= 1) return 0f

        var crossings = 0
        for (index in 1 until count) {
            val previous = samples[index - 1]
            val current = samples[index]
            if ((previous >= 0 && current < 0) || (previous < 0 && current >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / (count - 1)
    }

    /** 原地迭代式 Radix-2 Cooley-Tukey FFT，[re]/[im] 长度必须为 2 的整数次幂。 */
    private fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tempRe = re[i]; re[i] = re[j]; re[j] = tempRe
                val tempIm = im[i]; im[i] = im[j]; im[j] = tempIm
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2 * PI / len
            val baseWr = cos(angle)
            val baseWi = sin(angle)
            var i = 0
            while (i < n) {
                var curWr = 1.0
                var curWi = 0.0
                for (k in 0 until len / 2) {
                    val evenIndex = i + k
                    val oddIndex = evenIndex + len / 2
                    val oddRe = re[oddIndex] * curWr - im[oddIndex] * curWi
                    val oddIm = re[oddIndex] * curWi + im[oddIndex] * curWr
                    val evenRe = re[evenIndex]
                    val evenIm = im[evenIndex]
                    re[evenIndex] = evenRe + oddRe
                    im[evenIndex] = evenIm + oddIm
                    re[oddIndex] = evenRe - oddRe
                    im[oddIndex] = evenIm - oddIm

                    val nextWr = curWr * baseWr - curWi * baseWi
                    val nextWi = curWr * baseWi + curWi * baseWr
                    curWr = nextWr
                    curWi = nextWi
                }
                i += len
            }
            len = len shl 1
        }
    }
}

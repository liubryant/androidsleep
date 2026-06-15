package cn.cjym.timesleep.service

import java.io.File
import java.io.RandomAccessFile

/**
 * 极简 PCM 16-bit 单声道 WAV 文件写入器，用于将睡眠监测期间录制的音频落盘，
 * 对应 iOS `AVAudioRecorder` 的录音文件输出。
 */
class WavFileWriter(
    private val file: File,
    private val sampleRate: Int,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16,
) {
    private var raf: RandomAccessFile? = null
    private var dataLength = 0L

    fun open() {
        file.parentFile?.mkdirs()
        val f = RandomAccessFile(file, "rw")
        f.setLength(0)
        writeHeader(f, 0)
        raf = f
    }

    fun write(buffer: ShortArray, length: Int) {
        val f = raf ?: return
        val bytes = ByteArray(length * 2)
        for (i in 0 until length) {
            val value = buffer[i].toInt()
            bytes[i * 2] = (value and 0xFF).toByte()
            bytes[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
        }
        f.write(bytes)
        dataLength += bytes.size
    }

    fun close() {
        val f = raf ?: return
        writeHeader(f, dataLength)
        f.close()
        raf = null
    }

    private fun writeHeader(f: RandomAccessFile, dataLength: Long) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        f.seek(0)
        f.writeBytes("RIFF")
        writeIntLE(f, (36 + dataLength).toInt())
        f.writeBytes("WAVE")
        f.writeBytes("fmt ")
        writeIntLE(f, 16)
        writeShortLE(f, 1) // PCM
        writeShortLE(f, channels)
        writeIntLE(f, sampleRate)
        writeIntLE(f, byteRate)
        writeShortLE(f, blockAlign)
        writeShortLE(f, bitsPerSample)
        f.writeBytes("data")
        writeIntLE(f, dataLength.toInt())
        f.seek(44 + dataLength)
    }

    private fun writeIntLE(f: RandomAccessFile, value: Int) {
        f.write(
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
                ((value shr 16) and 0xFF).toByte(),
                ((value shr 24) and 0xFF).toByte(),
            ),
        )
    }

    private fun writeShortLE(f: RandomAccessFile, value: Int) {
        f.write(
            byteArrayOf(
                (value and 0xFF).toByte(),
                ((value shr 8) and 0xFF).toByte(),
            ),
        )
    }
}

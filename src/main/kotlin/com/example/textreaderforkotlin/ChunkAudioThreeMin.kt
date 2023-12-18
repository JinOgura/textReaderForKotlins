package com.example.textreaderforkotlin

import java.io.File
import java.io.ByteArrayInputStream
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import javax.sound.sampled.AudioFileFormat
import java.io.FileInputStream
import java.io.IOException

fun main() {
    val inputFile = File("/Users/jin.ogura/Downloads/レコーディング.wav")
    val outputDirectory = File("/Users/jin.ogura/Desktop/")
    val chunkSize = 180.0
    convertM4AToWAV("/Users/jin.ogura/Downloads/レコーディング.m4a", "/Users/jin.ogura/Downloads/レコーディング.wav")
//    runChunk(inputFile, outputDirectory, chunkSize)
}

fun runChunk(inputFile: File, outputDirectory: File, chunkSize: Double) {
    val durationInSeconds = getWavDuration(inputFile)

    var startTimeInSeconds = 0.0
    var endTimeInSeconds = chunkSize

    var count = 1 // ファイルのカウントを1から始める

    try {
        while (durationInSeconds > startTimeInSeconds) {
            val outputFileName = "output_$count.wav"
            val outputFile = File(outputDirectory, outputFileName)

            if (endTimeInSeconds > durationInSeconds) {
                endTimeInSeconds = durationInSeconds
            }
            trimWavFile(inputFile, outputFile, startTimeInSeconds, endTimeInSeconds)

            startTimeInSeconds += chunkSize
            endTimeInSeconds += chunkSize
            count++
        }
        println("WAVファイルの切り取りが完了しました。" + (count - 1) + "個です。")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun trimWavFile(inputFile: File, outputFile: File, startTimeInSeconds: Double, endTimeInSeconds: Double) {
    val audioInputStream = AudioSystem.getAudioInputStream(inputFile)

    val audioFormat: AudioFormat = audioInputStream.format
    val frameSize = audioFormat.frameSize
    val targetFrameRate = audioFormat.frameRate
    val targetType = audioFormat.encoding

    val startFrame = (startTimeInSeconds * targetFrameRate).toLong()
    val endFrame = (endTimeInSeconds * targetFrameRate).toLong()
    val targetLength = (endFrame - startFrame).toInt() * frameSize
    val targetData = ByteArray(targetLength)
    // 移動したい範囲のみ読み込む
    audioInputStream.skip(startFrame * frameSize)
    audioInputStream.read(targetData, 0, targetLength)

    val targetAudioInputStream = AudioInputStream(
        ByteArrayInputStream(targetData),
        AudioFormat(
            targetType,
            targetFrameRate,
            audioFormat.sampleSizeInBits,
            audioFormat.channels,
            frameSize,
            targetFrameRate,
            false
        ),
        (targetLength / frameSize).toLong()
    )

    AudioSystem.write(targetAudioInputStream, AudioSystem.getAudioFileFormat(inputFile).type, outputFile)
}

fun getWavDuration(file: File): Double {
    val audioInputStream = AudioSystem.getAudioInputStream(file)
    val audioFormat = audioInputStream.format

    // フレーム数とフレームレートから長さ（秒単位）を計算
    val frameLength = audioInputStream.frameLength
    val frameRate = audioFormat.frameRate
    val durationInSeconds = frameLength / frameRate

    return durationInSeconds.toDouble()
}

fun convertM4AToWAV(inputFilePath: String, outputFilePath: String): String {
    try {
        // M4Aファイルを読み込む
        val m4aFile = File(inputFilePath)
        val m4aInputStream = FileInputStream(m4aFile)
        val mpegAudioFileReader = MpegAudioFileReader()
        val audioInputStream = mpegAudioFileReader.getAudioInputStream(m4aInputStream)

        // WAVファイルに変換して保存
        val wavFile = File(outputFilePath)
        val wavAudioInputStream = AudioInputStream(audioInputStream, audioInputStream.format, audioInputStream.frameLength)
        AudioSystem.write(wavAudioInputStream, AudioFileFormat.Type.WAVE, wavFile)

        // ストリームを閉じる
        m4aInputStream.close()
        audioInputStream.close()
        wavAudioInputStream.close()

        println("変換が完了しました: $outputFilePath")
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return outputFilePath
}
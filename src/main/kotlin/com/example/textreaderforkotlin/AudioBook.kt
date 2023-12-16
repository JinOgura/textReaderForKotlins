package com.example.textreaderforkotlin

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLEncoder

fun main() {
    val inputFilePath = "C:\\Users\\rkfeo\\Downloads\\名称未設定2.txt"
    val language = "korean"
    val audioBook = AudioBook(inputFilePath, null, language)
    audioBook.generateAudioBook()
}

class AudioBook(private val inputFilePath: String?, private val textRaw: String?, private val language: String) {
    private val client = OkHttpClient()
    private val audioFiles = mutableListOf<File>()
    private val outputFilePath = "${inputFilePath?.substringBeforeLast('.')}.mp3"

    private fun downloadAndSaveAudioChunks(textChunks: List<String>) {
        for ((index, chunk) in textChunks.withIndex()) {
            val encodedText = URLEncoder.encode(chunk, "UTF-8")
            val lg: String = if (language == "korean") {
                "ko"
            } else {
                "ja"
            }
            val url =
                "https://translate.google.com/translate_tts?ie=UTF-8&q=$encodedText&tl=${lg}&total=${textChunks.size}&idx=$index&textlen=${chunk.length}&client=tw-ob&prev=input"

            val request = Request.Builder()
                .url(url)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body
                if (responseBody != null) {
                    val outputFile = File("C:\\Users\\rkfeo\\Downloads\\output_$index.mp3")
                    val outputStream = FileOutputStream(outputFile)
                    outputStream.write(responseBody.bytes())
                    outputStream.close()
                    audioFiles.add(outputFile)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun combineAudioChunks() {
        val outputStream = FileOutputStream(outputFilePath)
        for (file in audioFiles) {
            val inputFileBytes = file.readBytes()
            outputStream.write(inputFileBytes)
        }
        outputStream.close()
    }

    private fun deleteTempFiles() {
        audioFiles.forEach { it.delete() }
    }

    fun generateAudioBook() {
        val startTime = System.currentTimeMillis()
        var inputFile = ""
        if (inputFilePath != null) {
            inputFile = File(inputFilePath).readText()
        } else if (textRaw != null) {
            inputFile = textRaw
        }
        val chunkSize = 200
        val textChunks = inputFile.chunked(chunkSize)
        downloadAndSaveAudioChunks(textChunks)
        combineAudioChunks()
        deleteTempFiles()
        val chromePath = "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe"

        try {
            val file = File(outputFilePath)
            if (file.exists()) {
                val processBuilder = ProcessBuilder(chromePath, file.absolutePath)
                processBuilder.start()

//                Thread.sleep(5000)
//                file.delete()
            } else {
                println("指定したファイルは存在しません。")
            }
        } catch (e: IOException) {
            println("Chromeを起動できませんでした。")
            e.printStackTrace()
        }
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - startTime
        if (inputFilePath != null) {
            val minutes = (elapsedTime / 1000) / 60
            val seconds = (elapsedTime / 1000) % 60
            println("処理が終了しました。処理時間: ${minutes}分 ${seconds}秒")
        }
    }
}

package com.example.textreaderforkotlin
import java.io.File
import java.io.FileWriter

fun main() {
    val directoryPath = "C:\\Users\\rkfeo\\Downloads\\今日の考察\\勉強\\一時"
    val outputFile = "C:\\Users\\rkfeo\\Downloads\\今日の考察\\勉強\\一時\\merged.txt"

    val directory = File(directoryPath)
    val files = directory.listFiles { file -> file.isFile && file.extension == "txt" }

    if (files != null) {
        val mergedFileWriter = FileWriter(outputFile)

        for ((index, file) in files.withIndex()) {
            val content = file.readText()
            // 最初のファイル以外の場合、空白行を追加
            if (index != 0) {
                mergedFileWriter.write("\n\n")
            }
            mergedFileWriter.write(content)
        }

        mergedFileWriter.close()

        val mergedFile = File(outputFile)
        val characterCount = mergedFile.readText().length

        println("テキストファイルの統合が完了しました。統合したテキストファイルの文字数: $characterCount 文字")
    } else {
        println("指定されたディレクトリにテキストファイルが見つかりません。")
    }
}

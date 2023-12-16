package com.example.textreaderforkotlin

import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.stage.Stage
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import java.io.File
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.Node
import javafx.stage.Screen
import javafx.geometry.Insets

class TextReaderForKotlin : Application() {
    private var paragraphs: List<String> = mutableListOf()
    private var currentIndex = 0
    private val currentText = Text()
    private val allReadText = StringBuilder()
    private var filePath = "C:\\Users\\rkfeo\\Downloads\\今日の考察\\勉強\\merged.txt"
    private var rightClickCount = 0
    private var originalFontSize = 40.0
    private var messageBox = Rectangle(0.0, 0.0)
    private var endFlg = false

    override fun start(stage: Stage) {
        paragraphs = readParagraphs(filePath)

        if (paragraphs.isEmpty()) {
            println("ファイルが見つかりません: $filePath")
            Platform.exit()
            return
        }

        originalFontSize = 40.0

        val fontSize = 40.0
        currentText.font = Font.font(fontSize)
        val wrappingWidth = 1270.0
        currentText.wrappingWidth = wrappingWidth
        currentText.textAlignment = javafx.scene.text.TextAlignment.LEFT
        currentText.fill = Color.WHITE

        val scrollPane = ScrollPane()
        val textPane = StackPane(currentText)
        textPane.alignment = Pos.BOTTOM_CENTER

        val margin = 30.0
        StackPane.setMargin(textPane, Insets(0.0, 0.0, margin, 0.0))

        scrollPane.content = textPane

        val scene = Scene(scrollPane, 1280.0, 720.0)

        // 背景画像を設定する
        val backgroundImageUrl =
            "https://pbs.twimg.com/media/EIngPWeWwAA_P_6?format=jpg&name=large"

        val imageView = try {
            ImageView(Image(backgroundImageUrl))
        } catch (e: Exception) {
            ImageView()
        }

        val imagePane = StackPane(imageView)
        stage.isFullScreen = true
        val primaryScreen = Screen.getPrimary()
        val width = primaryScreen.visualBounds.width - 100.0

        val stackPane = StackPane(imagePane, createMessageBox(textPane, width))
        stackPane.alignment = Pos.CENTER

        scrollPane.content = stackPane
        scrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scrollPane.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER

        scene.setOnMouseClicked { event ->
            when (event.button) {
                MouseButton.PRIMARY -> {
                    imageView.isVisible = true
                    messageBox.isVisible = true
                    currentText.fill = Color.WHITE
                    showNextParagraph(scrollPane)
                }

                MouseButton.SECONDARY -> {
                    rightClickCount++
                    currentText.font = Font.font(fontSize)
                    imageView.isVisible = false
                    messageBox.isVisible = false
                    currentText.fill = Color.BLACK
                    if (rightClickCount == 1) showAllReadText(scrollPane)
                    else if (rightClickCount > 1) {
                        stage.isFullScreen = !stage.isFullScreen
                        rightClickCount = 0
                    }
                }

                else -> TODO()
            }
        }

        scene.setOnKeyPressed { event ->
            if (event.code == KeyCode.ENTER) {
                imageView.isVisible = true
                messageBox.isVisible = true
                currentText.fill = Color.WHITE
                showNextParagraph(scrollPane)
            }
        }

        stage.scene = scene

        // ウィンドウサイズ変更時に背景画像を拡大
        scene.widthProperty().addListener { _, _, newWidth ->
            imageView.fitWidth = newWidth.toDouble()
            messageBox.width = newWidth.toDouble() - 5.0
        }
        scene.heightProperty().addListener { _, _, newHeight ->
            imageView.fitHeight = newHeight.toDouble()
        }

        stage.show()
    }

    private fun createMessageBox(contentNode: Node, width: Double): StackPane {
        messageBox = Rectangle(width, 200.0)
        messageBox.fill = Color.color(0.0, 0.0, 0.0, 0.5) // 半透明の黒色

        val messageBoxPane = StackPane(messageBox, contentNode)
        messageBoxPane.alignment = Pos.BOTTOM_CENTER

        return messageBoxPane
    }

    private fun showNextParagraph(scrollPane: ScrollPane) {
        rightClickCount = 0
        if (currentIndex < paragraphs.size) {
            val paragraph = paragraphs[currentIndex]

            if (!endFlg) allReadText.append(paragraph.replace("\\n", "\n")).append("\n\n")

            currentText.text = paragraph.replace("\\n", "\n")

            currentText.wrappingWidth = scrollPane.width - 100.0 // 任意の余白を考慮

            val maxTextHeight = 170.0
            if (currentText.layoutBounds.height > maxTextHeight) {
                val fontSize = (maxTextHeight / currentText.layoutBounds.height) * originalFontSize
                currentText.font = Font.font(currentText.font.family, fontSize)
            } else {
                currentText.font = Font.font(currentText.font.family, originalFontSize)
            }
            currentIndex++
        } else {
            currentIndex = 0
            paragraphs = mutableListOf("END", "END", "END", "END")
            currentText.text = paragraphs[currentIndex]
            endFlg = true
        }
        scrollPane.layout()
        scrollPane.vvalue = 1.0
    }

    private fun readParagraphs(filePath: String): List<String> {
        val paragraphs = mutableListOf<String>()
        val file = File(filePath)

        if (file.exists()) {
            val fileContents = file.readText()
            val paragraphsRaw = fileContents.split(Regex("\\n\\s*\\n"))

            for (paragraph in paragraphsRaw) {
                paragraphs.add(paragraph)
            }
        }
        return paragraphs
    }

    private fun showAllReadText(scrollPane: ScrollPane) {
        currentText.text = allReadText.toString()

        scrollPane.layout()

        scrollPane.vvalue = scrollPane.vmax
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(TextReaderForKotlin::class.java)
        }
    }
}
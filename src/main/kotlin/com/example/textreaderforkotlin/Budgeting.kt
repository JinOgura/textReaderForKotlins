package com.example.textreaderforkotlin

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import javax.mail.*
import javax.mail.internet.*
import org.apache.commons.csv.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class FileInfo(
    val alreadySpentTitle: MutableList<String?>,
    val alreadySpentMoney: MutableList<Int>,
    val alreadySpentDate: MutableList<String?>,
    val dCardInfoTitle: MutableList<String>,
    val dCardInfoMoney: MutableList<Int>,
    val dCardInfoDate: MutableList<Date>
)

data class CsvData(
    val titles: MutableList<String>,
    val money: MutableList<Int>,
    val dates: MutableList<Date>
)

fun main() {
    Budgeting().run()
}

class Budgeting {
    private val setTime = "202312"
    private val getFileDateName = generateDateList(setTime)
    private val livingBudget = 200000
    private val sendMailFlg = false
    private val lineFlg = true
    private val userId = mapOf(
        "jin" to "U42074d31b46e7259875f4777dce83eb1",
        "honoka" to "U3a9070a4a77543224e2b0bcae38616d2"
    )
    private val lastWeek = "SUNDAY"
    private var lastMonthDate = 15
    private var housingCost = "95000"
    private var palSystem = "13000"
    private var jinCost = "15000"
    private var telCost = "8000"

    // 下のデータは基本修正しない
    private val charSet = Charset.forName("Shift-JIS")
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd")
    private var leftMoney = 0
    private val budgetingPath = "/Users/jin.ogura/Desktop/bugit/${getFileDateName[0]}/"
    private val alreadySpent = "${budgetingPath}使用履歴.xlsx"
    private val dCard = "${budgetingPath}${getFileDateName[2]}.csv"
    private val showDiffPath = "${budgetingPath}output.csv"

    fun run() {
        val fileInfo = getFileInfo()

        if (fileInfo.dCardInfoTitle.any { it.contains("賃料等") }) {
            housingCost = "確定"
        }
        if (fileInfo.dCardInfoTitle.any { it.contains("ドコモご利用料金") }) {
            telCost = "確定"
        }
        if (fileInfo.dCardInfoTitle.any { it.contains("パルシステム") }) {
            palSystem = "確定"
        }
        for (moneyAmount in fileInfo.alreadySpentMoney) {
            val i = fileInfo.dCardInfoMoney.indexOf(moneyAmount)
            if (i != -1) {
                fileInfo.dCardInfoTitle.removeAt(i)
                fileInfo.dCardInfoMoney.removeAt(i)
                fileInfo.dCardInfoDate.removeAt(i)
                continue
            }
        }

        val writer = FileWriter(showDiffPath, charSet)
        for (i in fileInfo.dCardInfoTitle.indices) {
            val formattedDate = dateFormat.format(fileInfo.dCardInfoDate[i])
            val line = "${fileInfo.dCardInfoTitle[i]},${fileInfo.dCardInfoMoney[i]},$formattedDate\n"
            writer.write(line)
        }
        writer.close()

        var outputResult = "家計簿とDカード利用明細に相違点あり"
        if (fileInfo.dCardInfoTitle.size == 0) {
            outputResult = "家計簿とDカード利用明細に相違点なし"
        }
        val paid = fileInfo.dCardInfoMoney.sum() + fileInfo.alreadySpentMoney.sum()
        val housingCostInt = housingCost.toIntOrNull() ?: 0
        val palSystemInt = palSystem.toIntOrNull() ?: 0
        val telCostInt = telCost.toIntOrNull() ?: 0
        val jinCostInt = jinCost.toIntOrNull() ?: 0
        leftMoney = livingBudget - paid - housingCostInt - palSystemInt - telCostInt - jinCostInt
        var result = "使える金額: ${leftMoney}円" +
                "\n使った金額: ${paid}円\n\n概算内容: 家賃等(${housingCost}), パルシステム(${palSystem}), 携帯料金(${telCost}),\n ジンお小遣い(${jinCost}(確定))\n\n" +
                outputResult
        println(result)
        val today: LocalDate = LocalDate.now()
        val todayDayAndDate = listOf(today.dayOfWeek.toString(), today.dayOfMonth)

        var resultMonth = ""
        var resultWeek = ""
        val resultInfo = updatePaidAlready()
        if (todayDayAndDate[0] == lastWeek) {
            val resultStringList = getResultStringList("weekend", resultInfo.titles, resultInfo.money, resultInfo.dates)
            resultWeek = "${resultStringList.joinToString(separator = "")}\n$result"
            println(resultWeek)
        }
        if (todayDayAndDate[1] == lastMonthDate) {
            val resultStringList =
                getResultStringList("monthend", resultInfo.titles, resultInfo.money, resultInfo.dates)
            resultMonth = "${resultStringList.joinToString(separator = "")}\n以上一ヶ月の決算です\n$result"
            println(resultMonth)
        }
        if (todayDayAndDate[1] != lastMonthDate && todayDayAndDate[0] != lastWeek) {
            result = getResultStringList("nothing", resultInfo.titles, resultInfo.money, resultInfo.dates)
                .joinToString(separator = "")
            println(result)
        }
        if (sendMailFlg) {
            if (todayDayAndDate[0] == lastWeek && resultWeek !== "") {
                sendEmail(resultWeek)
            }
            if (todayDayAndDate[1] == lastMonthDate) {
                sendEmail(resultMonth)
            }
            if (todayDayAndDate[0] != lastWeek && todayDayAndDate[1] != lastMonthDate && result != "") {
                sendEmail(result)
            }
        }
        if (lineFlg) {
            if (todayDayAndDate[0] == lastWeek && resultWeek !== "") {
                sendMessageToLineBot(userId["jin"], resultWeek.replace("\n", """\n"""))
                sendMessageToLineBot(userId["honoka"], resultWeek.replace("\n", """\n"""))
            }
            if (todayDayAndDate[1] == lastMonthDate) {
                sendMessageToLineBot(userId["jin"], resultMonth.replace("\n", """\n"""))
                sendMessageToLineBot(userId["honoka"], resultMonth.replace("\n", """\n"""))
            }
            if (todayDayAndDate[1] != lastMonthDate && todayDayAndDate[0] != lastWeek && result != "") {
                sendMessageToLineBot(userId["jin"], result.replace("\n", """\n"""))
                sendMessageToLineBot(userId["honoka"], result.replace("\n", """\n"""))
            }
        }
    }

    private fun updatePaidAlready(): CsvData {
        val fileInfo = getFileInfo()

        for (moneyAmount in fileInfo.dCardInfoMoney) {
            val i = fileInfo.alreadySpentMoney.indexOf(moneyAmount)
            if (i != -1) {
                fileInfo.alreadySpentTitle.removeAt(i)
                fileInfo.alreadySpentMoney.removeAt(i)
                fileInfo.alreadySpentDate.removeAt(i)
                continue
            }
        }

        val workbook = XSSFWorkbook()
        val sheets = workbook.createSheet("Sheet1")

        val titles = mutableListOf<String>()
        val numbers = mutableListOf<Int>()
        val dates = mutableListOf<Date>()

        for (i in fileInfo.dCardInfoTitle.indices) {
            val formattedDate = dateFormat.format(fileInfo.dCardInfoDate[i])
            val row = sheets.createRow(i)
            row.createCell(0).setCellValue(fileInfo.dCardInfoTitle[i])
            row.createCell(1).setCellValue(fileInfo.dCardInfoMoney[i].toDouble())
            row.createCell(2).setCellValue(formattedDate)

            dates.add(fileInfo.dCardInfoDate[i])
            numbers.add(fileInfo.dCardInfoMoney[i])
            titles.add(fileInfo.dCardInfoTitle[i])
        }

        for (i in fileInfo.alreadySpentMoney.indices) {
            val row = sheets.createRow(i + fileInfo.dCardInfoTitle.size + 1)
            row.createCell(0).setCellValue(fileInfo.alreadySpentTitle[i])
            row.createCell(1).setCellValue(fileInfo.alreadySpentMoney[i].toDouble())
            row.createCell(2).setCellValue(fileInfo.alreadySpentDate[i])

            dates.add(dateFormat.parse(fileInfo.alreadySpentDate[i]))
            numbers.add(fileInfo.alreadySpentMoney[i])
            fileInfo.alreadySpentTitle[i]?.let { titles.add(it) }
        }

        val fileOut = FileOutputStream(alreadySpent)
        workbook.write(fileOut)
        fileOut.close()
        workbook.close()

        return CsvData(titles, numbers, dates)
    }

    private fun getFileInfo(): FileInfo {
        val sheet = WorkbookFactory.create(File(alreadySpent)).getSheetAt(0)

        val title = mutableListOf<String?>()
        val money = mutableListOf<Int>()
        val dates = mutableListOf<String?>()

        for (row in sheet) {
            val cellA = row.getCell(0) // A列のセル（0から始まるインデックスで指定）
            val cellB = row.getCell(1) // B列のセル（0から始まるインデックスで指定）
            val cellC = row.getCell(2) // C列のセル（0から始まるインデックスで指定）

            if (cellA != null && cellA.cellType == org.apache.poi.ss.usermodel.CellType.STRING) {
                title.add(cellA.stringCellValue)
            } else {
                title.add(null)
            }

            if (cellB != null && cellB.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
                money.add(cellB.numericCellValue.toInt())
            }

            if (cellC != null) {
                dates.add(
                    if (cellC.cellType == org.apache.poi.ss.usermodel.CellType.NUMERIC) dateFormat.format(cellC.dateCellValue)
                    else cellC.stringCellValue
                )
            } else {
                dates.add(null)
            }
        }

        val csvInfo = getCsv()
        return FileInfo(title, money, dates, csvInfo.titles, csvInfo.money, csvInfo.dates)
    }

    private fun getCsv(): CsvData {
        val csvFile = File(dCard)
        val titles = mutableListOf<String>()
        val numbers = mutableListOf<Int>()
        val dates = mutableListOf<Date>()

        // CSVファイルをパースしてB列（タイトル）とG列（金額）とA列（日付）の値をリストに追加する
        CSVParser.parse(csvFile, charSet, CSVFormat.DEFAULT)
            .use { csvParser ->
                for (record in csvParser) {
                    val name = record.get(1)
                    val number = record.get(6)
                    val dateString = record.get(0)
                    if (name.isNotBlank() && number.isNotBlank() && dateString.isNotBlank()) {
                        val date = dateFormat.parse(dateString)
                        titles.add(name)
                        numbers.add(number.toInt())
                        dates.add(date)
                    }
                }
            }

        // 日付を基準にソートする
        val sortedIndices = dates.indices.sortedBy { dates[it] }
        val sortedTitles = sortedIndices.map { titles[it] }.toMutableList()
        val sortedNumbers = sortedIndices.map { numbers[it] }.toMutableList()
        val sortedDates = sortedIndices.map { dates[it] }.toMutableList()

        return CsvData(sortedTitles, sortedNumbers, sortedDates)
    }

    private fun sendEmail(body: String) {
        val toEmail = "dlfqhsdjqhzk@naver.com"
        val fromEmail = "dlfqhsdjqhzk@naver.com"
        val password = "65pyzr"
        val subject = "現在の残高"
        val properties = Properties()
        properties["mail.smtp.auth"] = "true"
        properties["mail.smtp.starttls.enable"] = "true"
        properties["mail.smtp.host"] = "smtp.naver.com"
        properties["mail.smtp.port"] = "587"

        val session = Session.getInstance(properties,
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(fromEmail, password)
                }
            })

        try {
            val message = MimeMessage(session)
            message.setFrom(InternetAddress(fromEmail))
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            message.subject = subject
            message.setText(body)

            Transport.send(message)
        } catch (e: Exception) {
            println("メールの送信中にエラーが発生しました: $e")
        }
    }

    private fun sendMessageToLineBot(userId: String?, message: String) {
        val url = URL("https://api.line.me/v2/bot/message/push")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty(
            "Authorization",
            "Bearer e6Zv9Boyr5u9PLZ3ezmDXpN6ViJ/V5hknBHvNLasJMLDy5DLNn0k4ylfGtvAIPDgQ0d8xBKGnSwJbkajpMyAMzs4ISiZ+UPETzDz0+6H7UHEl9quSdqcwv+MKIH6pXbsRFYg4hYvAthhGVC5qQ229QdB04t89/1O/w1cDnyilFU="
        )
        connection.doOutput = true
        val jsonInputString =
            """{ "to": "$userId", "messages": [{ "type": "text", "text": "$message" }] }"""

        val os = OutputStreamWriter(connection.outputStream)
        os.write(jsonInputString)
        os.flush()
        connection.responseCode
        connection.inputStream.bufferedReader().use { it.readText() }
        connection.disconnect()
    }

    private fun getRemainDays(): Int {
        val currentDateTime = LocalDate.now()
        val current15th = currentDateTime.withDayOfMonth(lastMonthDate)
        val targetDate = if (currentDateTime.dayOfMonth < lastMonthDate) {
            current15th
        } else {
            current15th.plusMonths(1)
        }
        return ChronoUnit.DAYS.between(currentDateTime, targetDate).toInt()
    }

    private fun generateDateList(startDate: String): List<String> {
        val dateFormat = SimpleDateFormat("yyyyMM", Locale.getDefault())
        val startDateObj: Date = dateFormat.parse(startDate) ?: Date()

        val calendar = Calendar.getInstance()
        calendar.time = startDateObj

        val dateList = mutableListOf<String>()
        repeat(3) {
            dateList.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.MONTH, 1)
        }

        return dateList
    }

    private fun getResultStringList(
        nowIsWhat: String,
        titles: MutableList<String>,
        numbers: MutableList<Int>,
        dates: MutableList<Date>
    ): MutableList<String> {
        val currentDate = Date() // 現在の日付を取得

        val cal = Calendar.getInstance()
        cal.time = currentDate
        cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var weeklyTotalAmount = 0 // 一週間の金額の総額
        val stringList = mutableListOf<String>()

        // 今日が日曜日の場合、その週の月曜日から日曜日までの範囲を取得
        if (nowIsWhat == "weekend") {
            val currentDateTime = LocalDate.now()
            // 実行した日が月の最後の日であり、新しい家計簿ファイルではない場合、空白を返す。
            if (currentDateTime.dayOfMonth == 15 && currentDateTime.minusMonths(1).monthValue == setTime.takeLast(2)
                    .toInt()
            ) {
                return stringList
            }
            cal.add(Calendar.DAY_OF_WEEK, -6) // 今日から週の始め（月曜日）まで戻る
            val startDate = cal.time // 週の始め（月曜日）
            cal.add(Calendar.DAY_OF_WEEK, 6) // 週の終わり（日曜日）まで進む
            val endDate = cal.time // 週の終わり（日曜日）

            val groupedByDate = dates.indices
                .filter {
                    val date = dates[it]
                    date in startDate..endDate
                }
                .groupBy {
                    val formattedDate = dateFormat.format(dates[it])
                    val dayOfWeek = SimpleDateFormat("E", Locale.getDefault()).format(dates[it])
                    "$formattedDate($dayOfWeek)"
                }

            // 日付でソート
            val sortedGrouped = groupedByDate.toList().sortedBy { (date, _) ->
                SimpleDateFormat("yyyy/MM/dd(E)", Locale.getDefault()).parse(date)
            }.toMap()

            for ((date, indices) in sortedGrouped) {
                stringList.add("$date\n")

                var dailyTotalAmount = 0 // 日ごとの金額の総額

                for (index in indices) {
                    val title = titles[index]
                    val amount = numbers[index]
                    stringList.add("$title: ${amount}円\n")
                    dailyTotalAmount += amount
                }

                stringList.add("\n")
                weeklyTotalAmount += dailyTotalAmount
            }

            // 一週間の金額の総額を結果に追加
            stringList.add("一週間の使用総額：${weeklyTotalAmount}円\n")
            val remainDays = getRemainDays()
            var multiple = 7
            if (remainDays < 7) {
                multiple = remainDays
            }
            val nextWeekAllowance = leftMoney / remainDays * multiple
            val stringEndDate = SimpleDateFormat("yyyy/MM/dd").format(endDate)
            stringList.add("次の週に使える金額：${nextWeekAllowance}\n")
            val outputFile = File("${budgetingPath}weekEndResult.txt")
            outputFile.printWriter().use { writer ->
                writer.println(stringEndDate)
                writer.println(nextWeekAllowance) // 整数に変換して書き込み
            }
        } else if (nowIsWhat == "monthend") {
            // 今日が日曜日でない場合は、通常の週の範囲を取得
            val groupedByDate = dates.indices
                .groupBy {
                    val formattedDate = dateFormat.format(dates[it])
                    val dayOfWeek = SimpleDateFormat("E", Locale.getDefault()).format(dates[it])
                    "$formattedDate($dayOfWeek)"
                }

            // 日付でソート
            val sortedGrouped = groupedByDate.toList().sortedBy { (date, _) ->
                SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(date)
            }.toMap()

            for ((date, indices) in sortedGrouped) {
                stringList.add("$date\n")

                for (index in indices) {
                    val title = titles[index]
                    val amount = numbers[index]
                    stringList.add("$title: ${amount}円\n")
                }
                stringList.add("\n")
            }
        } else if (nowIsWhat == "nothing") {
            val lines: List<String>
            try {
                lines = File("${budgetingPath}weekendResult.txt").readLines()
                // ファイルが存在する場合の処理
            } catch (e: Exception) {
                System.err.println("エラー: weekendResult.txtが存在しません")
                return stringList
            }
            val date = lines[0].trim()
            val value = lines[1].trim()
            val referenceDate = SimpleDateFormat("yyyy/MM/dd").parse(date)
            for (i in titles.indices) {
                if (titles[i].contains("賃料等") || titles[i].contains("ドコモご利用料金") || titles[i].contains("パルシステム")) {
                    continue
                }
                if (dates[i].after(referenceDate)) {
                    val amount = numbers[i]
                    weeklyTotalAmount += amount
                }
            }
            stringList.add("今週残ってる金額：${value.toInt() - weeklyTotalAmount}円")
        }
        return stringList
    }
}

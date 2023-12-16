package com.example.textreaderforkotlin

import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import javafx.stage.Stage
import kotlin.math.pow

class LoanCalculatorApp : Application() {

    override fun start(primaryStage: Stage) {
        val grid = GridPane()
        grid.padding = Insets(20.0, 0.0, 0.0, 250.0)
        grid.hgap = 10.0
        grid.vgap = 10.0

        val loanAmountLabel = Label("借りた金額:")
        val loanAmountTextField = TextField()
        loanAmountTextField.prefWidth = 600.0
        grid.add(loanAmountLabel, 0, 0)
        grid.add(loanAmountTextField, 1, 0)

        val annualInterestRateLabel = Label("金利（年利率）:")
        val annualInterestRateTextField = TextField()
        grid.add(annualInterestRateLabel, 0, 1)
        grid.add(annualInterestRateTextField, 1, 1)

        val numberOfYearsLabel = Label("返済期間（何年で返すか）:")
        val numberOfYearsTextField = TextField()
        grid.add(numberOfYearsLabel, 0, 2)
        grid.add(numberOfYearsTextField, 1, 2)

        val calculateButton = Button("計算")
        val resultTextArea = TextArea()
        resultTextArea.prefHeight = 500.0
        grid.add(calculateButton, 1, 3)
        grid.add(resultTextArea, 0, 4, 2, 1)

        calculateButton.setOnAction {
            val loanAmount = loanAmountTextField.text.toDouble()
            val annualInterestRate = annualInterestRateTextField.text.toDouble()/100
            val numberOfYears = numberOfYearsTextField.text.toInt()
            val numberOfMonths = numberOfYears * 12

            val monthlyInterestRate = annualInterestRate / 12
            val monthlyPayment = loanAmount * (monthlyInterestRate / (1 - (1 + monthlyInterestRate).pow(-numberOfMonths)))

            var remainingLoanAmount = loanAmount
            var years = 0
            var months = 0
            val resultText = StringBuilder()

            for (month in 1..numberOfMonths) {
                val monthlyInterestPayment = remainingLoanAmount * monthlyInterestRate
                val monthlyPrincipalPayment = monthlyPayment - monthlyInterestPayment

                remainingLoanAmount -= monthlyPrincipalPayment

                months++
                if (months == 12) {
                    years++
                    months = 0
                }

                resultText.append("${years}年${months}ヶ月: 月々の返済額: $monthlyPayment, 残りの返済額: $remainingLoanAmount\n")
            }

            resultTextArea.text = resultText.toString()
        }

        val scene = Scene(grid, 1280.0, 720.0)
        primaryStage.title = "ローン返済計算機"
        primaryStage.scene = scene
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(LoanCalculatorApp::class.java, *args)
        }
    }
}

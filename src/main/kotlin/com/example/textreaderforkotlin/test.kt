package com.example.textreaderforkotlin

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun getRemainDays(): Int {
    val currentDateTime = LocalDate.now()
    val current16th = currentDateTime.withDayOfMonth(20)
    val targetDate = current16th
//    val targetDate = if (currentDateTime.dayOfMonth < 15 + 1) {
//        current16th
//    } else {
//        current16th.plusMonths(1)
//    }
    println(currentDateTime)
    println(targetDate)
    println(ChronoUnit.DAYS.between(currentDateTime, targetDate).toInt())
    return ChronoUnit.DAYS.between(currentDateTime, targetDate).toInt()
}

fun main() {
    getRemainDays()
}
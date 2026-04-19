package com.paisa.app.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun Long.formatInr(): String {
    val rupees = this / 100.0
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return formatter.format(rupees)
}

fun Long.formatSignedInr(): String {
    val prefix = if (this >= 0) "+" else "-"
    return prefix + abs(this).formatInr()
}


package com.example.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {
    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Long, showSign: Boolean = false): String {
        val formatter = NumberFormat.getNumberInstance(indonesianLocale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        val formattedNumber = formatter.format(abs(amount))
        return when {
            amount < 0L -> "-Rp $formattedNumber"
            showSign && amount > 0L -> "+Rp $formattedNumber"
            else -> "Rp $formattedNumber"
        }
    }

    fun formatCompactRupiah(amount: Long): String {
        val absVal = abs(amount)
        val prefix = if (amount < 0L) "-Rp " else "Rp "
        return when {
            absVal >= 1_000_000_000L -> {
                val valB = absVal / 1_000_000_000.0
                "$prefix${trimDecimal(valB)}M"
            }
            absVal >= 1_000_000L -> {
                val valM = absVal / 1_000_000.0
                "$prefix${trimDecimal(valM)}jt"
            }
            absVal >= 1_000L -> {
                val valK = absVal / 1_000.0
                "$prefix${trimDecimal(valK)}rb"
            }
            else -> "$prefix$absVal"
        }
    }

    private fun trimDecimal(value: Double): String {
        val rounded = String.format(indonesianLocale, "%.1f", value)
        return rounded.removeSuffix(",0").removeSuffix(".0")
    }

    fun formatInputGrouping(digitsOnly: String): String {
        val clean = digitsOnly.filter { it.isDigit() }.trimStart('0')
        if (clean.isEmpty()) return ""
        val parsed = clean.toLongOrNull() ?: return ""
        val formatter = NumberFormat.getNumberInstance(indonesianLocale).apply {
            maximumFractionDigits = 0
        }
        return formatter.format(parsed)
    }

    fun parseAmount(input: String): Long {
        val digits = input.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }
}

package com.example.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CurrencyOption(val code: String, val symbolAr: String, val labelAr: String) {
    EGP("EGP", "ج.م", "الجنيه المصري (EGP)"),
    SAR("SAR", "ر.س", "الريال السعودي (SAR)"),
    AED("AED", "د.إ", "الدرهم الإماراتي (AED)"),
    KWD("KWD", "د.ك", "الدينار الكويتي (KWD)"),
    USD("USD", "$", "الدولار الأمريكي (USD)"),
    EUR("EUR", "€", "اليورو (EUR)");

    companion object {
        fun fromCode(code: String): CurrencyOption =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EGP
    }
}

object MoneyUtils {
    private val numberFormatter: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        DecimalFormat("#,##0.00", symbols)
    }

    fun parseAmountToPiasters(input: String): Long? {
        val normalized = input.trim()
            .replace("٬", "")
            .replace(",", "")
            .replace("٫", ".")
            .map { ch ->
                when (ch) {
                    in '٠'..'٩' -> ('0' + (ch - '٠'))
                    else -> ch
                }
            }
            .joinToString("")
        if (normalized.isBlank()) return null
        return try {
            val bd = BigDecimal(normalized)
            if (bd <= BigDecimal.ZERO) null
            else bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun parseNonNegativeToPiasters(input: String): Long? {
        val normalized = input.trim()
            .replace("٬", "")
            .replace(",", "")
            .replace("٫", ".")
            .map { ch ->
                when (ch) {
                    in '٠'..'٩' -> ('0' + (ch - '٠'))
                    else -> ch
                }
            }
            .joinToString("")
        if (normalized.isBlank()) return 0L
        return try {
            val bd = BigDecimal(normalized)
            if (bd < BigDecimal.ZERO) null
            else bd.multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun piastersToInputString(piasters: Long): String {
        if (piasters == 0L) return ""
        val bd = BigDecimal(piasters).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        return bd.stripTrailingZeros().toPlainString()
    }

    fun formatCurrency(piasters: Long, currencyCode: String = "EGP"): String {
        val currency = CurrencyOption.fromCode(currencyCode)
        val amount = BigDecimal(piasters).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
        val sign = if (piasters < 0) "-" else ""
        val absText = numberFormatter.format(amount.abs())
        return "$sign$absText ${currency.symbolAr}"
    }

    fun formatCompactCurrency(piasters: Long, currencyCode: String = "EGP"): String {
        val currency = CurrencyOption.fromCode(currencyCode)
        val egp = piasters / 100.0
        return if (kotlin.math.abs(egp) >= 1000) {
            String.format(Locale.US, "%.1f ألف %s", egp / 1000.0, currency.symbolAr)
        } else {
            formatCurrency(piasters, currencyCode)
        }
    }

    fun currentYearMonth(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun previousYearMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        return if (month <= 1) {
            String.format(Locale.US, "%04d-12", year - 1)
        } else {
            String.format(Locale.US, "%04d-%02d", year, month - 1)
        }
    }

    fun nextYearMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        return if (month >= 12) {
            String.format(Locale.US, "%04d-01", year + 1)
        } else {
            String.format(Locale.US, "%04d-%02d", year, month + 1)
        }
    }

    fun yearMonthLabelAr(yearMonth: String): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0) ?: ""
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val names = listOf(
            "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
            "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
        )
        val monthName = names.getOrElse(month - 1) { yearMonth }
        return "$monthName $year"
    }

    fun formatDateAr(epochMillis: Long): String {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale("ar", "EG"))
        return fmt.format(Date(epochMillis))
    }

    fun formatShortDateAr(epochMillis: Long): String {
        val fmt = SimpleDateFormat("dd MMM", Locale("ar", "EG"))
        return fmt.format(Date(epochMillis))
    }

    fun daysUntil(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        if (epochMillis <= 0L) return Long.MAX_VALUE
        val diff = epochMillis - nowMillis
        return diff / (1000L * 60L * 60L * 24L)
    }

    fun startOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfWeekMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return nowMillis - (7L * 24L * 60L * 60L * 1000L)
    }
}

package com.example.util

import com.example.domain.model.CyclePeriod
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object CycleDateHelper {
    private val idLocale = Locale("id", "ID")

    /**
     * Computes a custom monthly cycle period (e.g., 26 Oct - 25 Nov when startDay = 26,
     * or 1 Nov - 30 Nov when startDay = 1).
     *
     * Explicit bounds [startMillis, endMillis] ensure transactions on the boundary dates
     * (00:00:00.000 of start day through 23:59:59.999 of end day) are strictly included.
     */
    fun getCyclePeriod(
        cycleStartDay: Int,
        offsetFromCurrent: Int = 0,
        referenceMillis: Long = System.currentTimeMillis(),
    ): CyclePeriod {
        val clampedConfigDay = cycleStartDay.coerceIn(1, 31)
        val refCal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val refYear = refCal.get(Calendar.YEAR)
        val refMonth = refCal.get(Calendar.MONTH)
        val refDay = refCal.get(Calendar.DAY_OF_MONTH)

        val currentMonthStartDay = effectiveDayForMonth(refYear, refMonth, clampedConfigDay)
        val anchorCal = Calendar.getInstance().apply {
            clear()
            set(refYear, refMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            if (refDay < currentMonthStartDay) {
                add(Calendar.MONTH, -1)
            }
            add(Calendar.MONTH, offsetFromCurrent)
        }

        val startYear = anchorCal.get(Calendar.YEAR)
        val startMonth = anchorCal.get(Calendar.MONTH)
        val actualStartDay = effectiveDayForMonth(startYear, startMonth, clampedConfigDay)

        val startCal = Calendar.getInstance().apply {
            clear()
            set(startYear, startMonth, actualStartDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextMonthCal = Calendar.getInstance().apply {
            clear()
            set(startYear, startMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, 1)
        }
        val nextYear = nextMonthCal.get(Calendar.YEAR)
        val nextMonth = nextMonthCal.get(Calendar.MONTH)
        val nextActualStartDay = effectiveDayForMonth(nextYear, nextMonth, clampedConfigDay)

        val nextCycleStartCal = Calendar.getInstance().apply {
            clear()
            set(nextYear, nextMonth, nextActualStartDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startMillis = startCal.timeInMillis
        val endMillis = nextCycleStartCal.timeInMillis - 1L

        val label = formatCycleLabel(startMillis, endMillis)
        return CyclePeriod(
            startMillis = startMillis,
            endMillis = endMillis,
            label = label,
            offsetFromCurrent = offsetFromCurrent,
        )
    }

    private fun effectiveDayForMonth(year: Int, month: Int, desiredDay: Int): Int {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month, 1)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return desiredDay.coerceIn(1, maxDay)
    }

    fun formatCycleLabel(startMillis: Long, endMillis: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
        val sameMonth = sameYear && startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)

        val dayFmt = SimpleDateFormat("d", idLocale)
        val dayMonthFmt = SimpleDateFormat("d MMM", idLocale)
        val fullFmt = SimpleDateFormat("d MMM yyyy", idLocale)

        return when {
            sameMonth -> "${dayFmt.format(Date(startMillis))} – ${fullFmt.format(Date(endMillis))}"
            sameYear -> "${dayMonthFmt.format(Date(startMillis))} – ${fullFmt.format(Date(endMillis))}"
            else -> "${fullFmt.format(Date(startMillis))} – ${fullFmt.format(Date(endMillis))}"
        }
    }

    fun getLastNCyclePeriods(
        cycleStartDay: Int,
        count: Int = 6,
        endOffset: Int = 0,
        referenceMillis: Long = System.currentTimeMillis(),
    ): List<CyclePeriod> {
        val safeCount = count.coerceAtLeast(1)
        return ((endOffset - safeCount + 1)..endOffset).map { offset ->
            getCyclePeriod(
                cycleStartDay = cycleStartDay,
                offsetFromCurrent = offset,
                referenceMillis = referenceMillis,
            )
        }
    }

    fun formatShortCycleMonth(cycleStartDay: Int, startMillis: Long, endMillis: Long): String {
        val anchorMillis = if (cycleStartDay >= 15) endMillis else startMillis
        val fmt = SimpleDateFormat("MMM", idLocale)
        return fmt.format(Date(anchorMillis))
    }

    fun formatFullDate(millis: Long): String {
        val fmt = SimpleDateFormat("EEE, d MMM yyyy", idLocale)
        return fmt.format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        val fmt = SimpleDateFormat("d MMM yyyy", idLocale)
        return fmt.format(Date(millis))
    }

    fun formatDayMonth(millis: Long): String {
        val fmt = SimpleDateFormat("d MMM", idLocale)
        return fmt.format(Date(millis))
    }

    fun formatDayNumber(millis: Long): String {
        val fmt = SimpleDateFormat("d", idLocale)
        return fmt.format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        val fmt = SimpleDateFormat("HH:mm", idLocale)
        return fmt.format(Date(millis))
    }

    fun formatIsoDateTime(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return fmt.format(Date(millis))
    }

    fun parseIsoDateTime(value: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm",
            "dd/MM/yyyy",
        )
        for (pattern in formats) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val date = fmt.parse(value.trim())
                if (date != null) return date.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return null
    }

    fun startOfDayMillis(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun enumerateDaysInCycle(startMillis: Long, endMillis: Long): List<Long> {
        val days = mutableListOf<Long>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(startMillis)
        }
        while (cal.timeInMillis <= endMillis) {
            days.add(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }

    fun relativeDateHeader(dayStartMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val todayStart = startOfDayMillis(nowMillis)
        val yesterdayStart = Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_MONTH, -1)
        }.timeInMillis
        return when (dayStartMillis) {
            todayStart -> "Hari Ini"
            yesterdayStart -> "Kemarin"
            else -> formatFullDate(dayStartMillis)
        }
    }
}

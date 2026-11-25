package com.gymtrainer.app.ui

import com.gymtrainer.app.domain.Units
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object Format {
    private const val KG_TO_LB = 2.2046226

    fun weight(kg: Double, units: Units): String = when (units) {
        Units.METRIC -> "${trim(kg)} kg"
        Units.IMPERIAL -> "${trim(kg * KG_TO_LB)} lb"
    }

    fun weightValue(kg: Double, units: Units): Double =
        if (units == Units.IMPERIAL) kg * KG_TO_LB else kg

    fun toKg(value: Double, units: Units): Double =
        if (units == Units.IMPERIAL) value / KG_TO_LB else value

    fun weightUnit(units: Units): String = if (units == Units.IMPERIAL) "lb" else "kg"

    fun height(cm: Double, units: Units): String = when (units) {
        Units.METRIC -> "${trim(cm)} cm"
        Units.IMPERIAL -> {
            val totalInches = (cm / 2.54).roundToInt()
            val ft = totalInches / 12
            val inch = totalInches % 12
            "$ft' $inch\""
        }
    }

    /** Millilitres as litres with up to 2 decimals: 250 → "0.25", 2500 → "2.5". */
    fun litres(ml: Int): String =
        String.format(Locale.US, "%.2f", ml / 1000.0).trimEnd('0').trimEnd('.')

    fun trim(v: Double): String =
        if (v == v.roundToInt().toDouble()) v.roundToInt().toString()
        else String.format(Locale.US, "%.1f", v)

    fun date(ms: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))

    fun shortDate(ms: Long): String =
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))

    fun duration(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    fun clock(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }
}

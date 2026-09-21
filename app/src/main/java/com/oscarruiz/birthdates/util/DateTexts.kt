package com.oscarruiz.birthdates.util

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Formatos de fecha adaptados al idioma del móvil (p. ej. "jue, 24 sept" / "Thu, Sep 24"). */
object DateTexts {

    private fun formatter(skeleton: String, locale: Locale = Locale.getDefault()): DateTimeFormatter =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)

    /** Fecha corta con día de la semana: "jue, 24 sept". */
    fun short(date: LocalDate): String = formatter("EEEdMMM").format(date)

    /** Fecha larga con día de la semana: "jueves, 24 de septiembre". */
    fun long(date: LocalDate): String = formatter("EEEEdMMMM").format(date)

    /** Fecha y hora cortas para "última copia". */
    fun dayMonthTime(date: LocalDate, time: LocalTime): String =
        formatter("dMMM").format(date) + " · " + DateTimeFormatter.ofPattern("HH:mm").format(time)

    fun monthName(month: Int, locale: Locale = Locale.getDefault()): String =
        Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    fun time(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)
}

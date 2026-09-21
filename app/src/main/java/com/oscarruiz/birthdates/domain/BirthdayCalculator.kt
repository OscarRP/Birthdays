package com.oscarruiz.birthdates.domain

import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.domain.model.LeapDayPolicy
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class UpcomingBirthday(
    val birthday: Birthday,
    val nextDate: LocalDate,
    val daysUntil: Int,
    /** Edad que cumplirá en [nextDate], o null si no se conoce el año. */
    val turningAge: Int?,
)

data class Reminder(
    val birthday: Birthday,
    val daysUntil: Int,
    val date: LocalDate,
    val turningAge: Int?,
)

/** Secciones de la pantalla de Inicio. */
enum class Section {
    TODAY, THIS_WEEK, NEXT_WEEKS, LATER;

    companion object {
        fun of(daysUntil: Int): Section = when {
            daysUntil == 0 -> TODAY
            daysUntil <= 7 -> THIS_WEEK
            daysUntil <= 30 -> NEXT_WEEKS
            else -> LATER
        }
    }
}

/** Lógica de fechas en Kotlin puro: sin Android, fácil de probar. */
object BirthdayCalculator {

    fun maxDay(month: Int, year: Int?): Int =
        if (year != null) YearMonth.of(year, month).lengthOfMonth() else Month.of(month).maxLength()

    fun isValidDate(day: Int, month: Int, year: Int?): Boolean {
        if (month !in 1..12 || day < 1) return false
        return day <= maxDay(month, year)
    }

    /** Fecha en la que se celebra el cumpleaños en [year], aplicando la regla del 29 de febrero. */
    fun occurrenceInYear(day: Int, month: Int, year: Int, policy: LeapDayPolicy): LocalDate {
        if (month == 2 && day == 29 && !Year.isLeap(year.toLong())) {
            return when (policy) {
                LeapDayPolicy.FEB_28 -> LocalDate.of(year, 2, 28)
                LeapDayPolicy.MAR_1 -> LocalDate.of(year, 3, 1)
            }
        }
        return LocalDate.of(year, month, day)
    }

    /** Próxima celebración a partir de [today], incluido hoy. */
    fun nextOccurrence(day: Int, month: Int, today: LocalDate, policy: LeapDayPolicy): LocalDate {
        val thisYear = occurrenceInYear(day, month, today.year, policy)
        return if (!thisYear.isBefore(today)) thisYear
        else occurrenceInYear(day, month, today.year + 1, policy)
    }

    fun upcoming(birthday: Birthday, today: LocalDate, policy: LeapDayPolicy): UpcomingBirthday {
        val next = nextOccurrence(birthday.day, birthday.month, today, policy)
        val days = ChronoUnit.DAYS.between(today, next).toInt()
        val age = birthday.year?.let { next.year - it }?.takeIf { it >= 0 }
        return UpcomingBirthday(birthday, next, days, age)
    }

    /** Ordenados por cercanía y, a igualdad de fecha, por nombre. */
    fun upcomingSorted(
        birthdays: List<Birthday>,
        today: LocalDate,
        policy: LeapDayPolicy,
    ): List<UpcomingBirthday> =
        birthdays.map { upcoming(it, today, policy) }
            .sortedWith(compareBy<UpcomingBirthday> { it.daysUntil }.thenBy { it.birthday.name.lowercase() })

    /** Avisos que tocan hoy según los días de antelación elegidos. */
    fun remindersFor(
        birthdays: List<Birthday>,
        today: LocalDate,
        offsets: Set<Int>,
        policy: LeapDayPolicy,
    ): List<Reminder> =
        upcomingSorted(birthdays, today, policy)
            .filter { it.daysUntil in offsets }
            .map { Reminder(it.birthday, it.daysUntil, it.nextDate, it.turningAge) }
}

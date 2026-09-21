package com.oscarruiz.birthdates

import com.oscarruiz.birthdates.domain.BirthdayCalculator
import com.oscarruiz.birthdates.domain.Section
import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.domain.model.LeapDayPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BirthdayCalculatorTest {

    private val policy = LeapDayPolicy.FEB_28

    @Test
    fun birthdayToday_isZeroDaysAway() {
        val today = LocalDate.of(2026, 9, 21)
        val result = BirthdayCalculator.upcoming(Birthday(name = "María", day = 21, month = 9, year = 1992), today, policy)
        assertEquals(0, result.daysUntil)
        assertEquals(34, result.turningAge)
    }

    @Test
    fun birthdayAlreadyPassed_movesToNextYear() {
        val today = LocalDate.of(2026, 9, 21)
        val result = BirthdayCalculator.upcoming(Birthday(name = "Ana", day = 20, month = 9, year = 2000), today, policy)
        assertEquals(LocalDate.of(2027, 9, 20), result.nextDate)
        assertEquals(364, result.daysUntil)
        assertEquals(27, result.turningAge)
    }

    @Test
    fun endOfYear_crossesIntoJanuary() {
        val today = LocalDate.of(2026, 12, 30)
        val result = BirthdayCalculator.upcoming(Birthday(name = "Luis", day = 2, month = 1), today, policy)
        assertEquals(LocalDate.of(2027, 1, 2), result.nextDate)
        assertEquals(3, result.daysUntil)
    }

    @Test
    fun unknownYear_hasNoAge() {
        val today = LocalDate.of(2026, 9, 21)
        val result = BirthdayCalculator.upcoming(Birthday(name = "Javier", day = 3, month = 11), today, policy)
        assertNull(result.turningAge)
    }

    @Test
    fun leapDay_inNonLeapYear_usesPolicy() {
        val today = LocalDate.of(2027, 1, 1)
        val b = Birthday(name = "Leap", day = 29, month = 2, year = 2000)
        assertEquals(LocalDate.of(2027, 2, 28), BirthdayCalculator.upcoming(b, today, LeapDayPolicy.FEB_28).nextDate)
        assertEquals(LocalDate.of(2027, 3, 1), BirthdayCalculator.upcoming(b, today, LeapDayPolicy.MAR_1).nextDate)
    }

    @Test
    fun leapDay_inLeapYear_isFeb29() {
        val today = LocalDate.of(2028, 1, 1)
        val b = Birthday(name = "Leap", day = 29, month = 2)
        assertEquals(LocalDate.of(2028, 2, 29), BirthdayCalculator.upcoming(b, today, policy).nextDate)
    }

    @Test
    fun leapDay_celebratedOnFeb28_isNotSkippedOnThatDay() {
        val today = LocalDate.of(2027, 2, 28)
        val b = Birthday(name = "Leap", day = 29, month = 2)
        assertEquals(0, BirthdayCalculator.upcoming(b, today, LeapDayPolicy.FEB_28).daysUntil)
    }

    @Test
    fun dateValidation() {
        assertTrue(BirthdayCalculator.isValidDate(29, 2, null))
        assertTrue(BirthdayCalculator.isValidDate(29, 2, 2000))
        assertFalse(BirthdayCalculator.isValidDate(29, 2, 2001))
        assertFalse(BirthdayCalculator.isValidDate(31, 4, null))
        assertFalse(BirthdayCalculator.isValidDate(0, 1, null))
        assertFalse(BirthdayCalculator.isValidDate(1, 13, null))
    }

    @Test
    fun reminders_matchSelectedOffsets() {
        val today = LocalDate.of(2026, 9, 21)
        val list = listOf(
            Birthday(name = "Today", day = 21, month = 9),
            Birthday(name = "Tomorrow", day = 22, month = 9),
            Birthday(name = "InThree", day = 24, month = 9),
            Birthday(name = "InSeven", day = 28, month = 9),
        )
        val due = BirthdayCalculator.remindersFor(list, today, setOf(0, 3), policy)
        assertEquals(listOf("Today", "InThree"), due.map { it.birthday.name })
    }

    @Test
    fun sorting_isByDaysThenName() {
        val today = LocalDate.of(2026, 9, 21)
        val list = listOf(
            Birthday(name = "Zoe", day = 25, month = 9),
            Birthday(name = "ana", day = 25, month = 9),
            Birthday(name = "Bea", day = 22, month = 9),
        )
        val sorted = BirthdayCalculator.upcomingSorted(list, today, policy).map { it.birthday.name }
        assertEquals(listOf("Bea", "ana", "Zoe"), sorted)
    }

    @Test
    fun sections() {
        assertEquals(Section.TODAY, Section.of(0))
        assertEquals(Section.THIS_WEEK, Section.of(7))
        assertEquals(Section.NEXT_WEEKS, Section.of(8))
        assertEquals(Section.NEXT_WEEKS, Section.of(30))
        assertEquals(Section.LATER, Section.of(31))
    }
}

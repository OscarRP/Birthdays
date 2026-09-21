package com.oscarruiz.birthdates.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscarruiz.birthdates.data.BirthdayRepository
import com.oscarruiz.birthdates.domain.BirthdayCalculator
import com.oscarruiz.birthdates.domain.model.Birthday
import com.oscarruiz.birthdates.domain.model.Relation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditUiState(
    val loading: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val day: Int = LocalDate.now().dayOfMonth,
    val month: Int = LocalDate.now().monthValue,
    val yearText: String = "",
    val unknownYear: Boolean = false,
    val relation: Relation? = null,
    val notes: String = "",
    val nameError: Boolean = false,
    val yearError: Boolean = false,
    val dateError: Boolean = false,
    val finished: Boolean = false,
) {
    val maxDay: Int
        get() = BirthdayCalculator.maxDay(month, if (unknownYear) null else yearText.toIntOrNull())
}

class EditViewModel(
    private val id: String?,
    private val birthdays: BirthdayRepository,
) : ViewModel() {

    private var original: Birthday? = null
    private val _state = MutableStateFlow(EditUiState(loading = id != null, isNew = id == null))
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (id != null) {
            viewModelScope.launch {
                val b = birthdays.get(id)
                original = b
                _state.value = if (b == null) {
                    EditUiState(isNew = true)
                } else {
                    EditUiState(
                        isNew = false,
                        name = b.name,
                        day = b.day,
                        month = b.month,
                        yearText = b.year?.toString().orEmpty(),
                        unknownYear = b.year == null,
                        relation = b.relation,
                        notes = b.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onName(value: String) = _state.update { it.copy(name = value, nameError = false) }

    fun onDay(value: Int) = _state.update { it.copy(day = value, dateError = false) }

    fun onMonth(value: Int) = _state.update {
        val updated = it.copy(month = value, dateError = false)
        updated.copy(day = updated.day.coerceAtMost(updated.maxDay))
    }

    fun onYear(value: String) = _state.update {
        it.copy(yearText = value.filter(Char::isDigit).take(4), yearError = false, dateError = false)
    }

    fun onUnknownYear(value: Boolean) = _state.update { it.copy(unknownYear = value, yearError = false) }

    fun onRelation(value: Relation?) = _state.update { it.copy(relation = value) }

    fun onNotes(value: String) = _state.update { it.copy(notes = value) }

    fun save() {
        val s = _state.value
        val name = s.name.trim()
        // Año vacío = desconocido, para poder guardar en pocos segundos.
        val year = if (s.unknownYear) null else s.yearText.toIntOrNull()
        val yearError = !s.unknownYear && s.yearText.isNotEmpty() &&
            (year == null || year !in 1900..LocalDate.now().year)
        val dateError = !yearError && !BirthdayCalculator.isValidDate(s.day, s.month, year)

        if (name.isEmpty() || yearError || dateError) {
            _state.update { it.copy(nameError = name.isEmpty(), yearError = yearError, dateError = dateError) }
            return
        }

        val base = original ?: Birthday(name = name, day = s.day, month = s.month)
        val birthday = base.copy(
            name = name,
            day = s.day,
            month = s.month,
            year = year,
            relation = s.relation,
            notes = s.notes.trim().ifEmpty { null },
        )
        viewModelScope.launch {
            birthdays.save(birthday)
            _state.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val b = original ?: return
        viewModelScope.launch {
            birthdays.delete(b.id)
            _state.update { it.copy(finished = true) }
        }
    }
}

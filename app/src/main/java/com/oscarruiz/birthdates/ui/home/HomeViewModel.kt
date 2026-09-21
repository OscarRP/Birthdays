package com.oscarruiz.birthdates.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscarruiz.birthdates.data.BirthdayRepository
import com.oscarruiz.birthdates.data.SettingsRepository
import com.oscarruiz.birthdates.domain.BirthdayCalculator
import com.oscarruiz.birthdates.domain.Section
import com.oscarruiz.birthdates.domain.UpcomingBirthday
import com.oscarruiz.birthdates.domain.model.Birthday
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HomeSection(val section: Section, val items: List<UpcomingBirthday>)

data class HomeUiState(
    val loading: Boolean = true,
    val total: Int = 0,
    val query: String = "",
    val sections: List<HomeSection> = emptyList(),
)

class HomeViewModel(
    private val birthdays: BirthdayRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val today = MutableStateFlow(LocalDate.now())
    private var lastDeleted: Birthday? = null

    val uiState: StateFlow<HomeUiState> = combine(
        birthdays.observeAll(), settings.settings, query, today,
    ) { list, appSettings, q, day ->
        val upcoming = BirthdayCalculator.upcomingSorted(list, day, appSettings.leapDayPolicy)
        val term = q.trim()
        val filtered = if (term.isEmpty()) upcoming else upcoming.filter {
            it.birthday.name.contains(term, ignoreCase = true) ||
                it.birthday.notes?.contains(term, ignoreCase = true) == true
        }
        HomeUiState(
            loading = false,
            total = list.size,
            query = q,
            // La lista ya está ordenada por días, así que groupBy conserva el orden de secciones.
            sections = filtered.groupBy { Section.of(it.daysUntil) }.map { (s, items) -> HomeSection(s, items) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** Llamar al volver a la app, por si ha cambiado el día. */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun delete(birthday: Birthday) {
        lastDeleted = birthday
        viewModelScope.launch { birthdays.delete(birthday.id) }
    }

    fun undoDelete() {
        val item = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { birthdays.restore(item) }
    }

    fun onAdsIntroShown() = viewModelScope.launch { settings.setAdsIntroShown() }
    fun onNotificationPermissionAsked() = viewModelScope.launch { settings.setNotificationPermissionAsked() }
    fun onProPromptShown() = viewModelScope.launch { settings.setProPromptShown() }
}

package com.d1onix.dishlab.feature.home.presentation.onboarding

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.d1onix.dishlab.domain.model.AllergenPreference
import com.d1onix.dishlab.domain.model.CookingPreferences
import com.d1onix.dishlab.domain.model.DietPreference
import com.d1onix.dishlab.domain.model.KitchenEquipment
import com.d1onix.dishlab.domain.model.TastePreference
import com.d1onix.dishlab.domain.repository.CookingPreferencesRepository
import com.d1onix.dishlab.feature.home.navigation.SettingsRouter
import com.d1onyx.core.presentation.CommonDependencies
import com.d1onyx.core.presentation.WithMviState
import com.d1onyx.core.presentation.base.AbstractViewModel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingSection { Diets, Allergens, Tastes, Equipment }

@Immutable
data class OnboardingUiState(
    val showIntro: Boolean = true,
    val section: OnboardingSection = OnboardingSection.Diets,
    val preferences: CookingPreferences = CookingPreferences(),
) {
    val step: Int get() = OnboardingSection.entries.indexOf(section) + 1
    val totalSteps: Int get() = OnboardingSection.entries.size
}

sealed interface OnboardingAction {
    data object StartClicked : OnboardingAction
    data object SkipClicked : OnboardingAction
    data object BackClicked : OnboardingAction
    data object NextClicked : OnboardingAction
    data class OptionToggled(val value: String) : OnboardingAction
}

@AssistedInject
class OnboardingViewModel(
    dependencies: CommonDependencies,
    @Assisted showIntro: Boolean,
    private val preferencesRepository: CookingPreferencesRepository,
    private val router: SettingsRouter,
) : AbstractViewModel(dependencies), WithMviState<OnboardingUiState> {
    private val mutableState = MutableStateFlow(OnboardingUiState(showIntro = showIntro))
    val uiState: StateFlow<OnboardingUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = preferencesRepository.preferences.first()
            mutableState.update { it.copy(preferences = saved) }
        }
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.StartClicked -> mutableState.update { it.copy(showIntro = false) }
            OnboardingAction.SkipClicked -> finish(save = false)
            OnboardingAction.BackClicked -> previousOrClose()
            OnboardingAction.NextClicked -> nextOrFinish()
            is OnboardingAction.OptionToggled -> toggle(action.value)
        }
    }

    private fun previousOrClose() {
        val state = mutableState.value
        if (state.showIntro || state.section == OnboardingSection.Diets) {
            router.goBack()
        } else {
            mutableState.update {
                it.copy(section = OnboardingSection.entries[it.step - 2])
            }
        }
    }

    private fun nextOrFinish() {
        val state = mutableState.value
        if (state.section == OnboardingSection.Equipment) {
            finish(save = true)
        } else {
            mutableState.update { it.copy(section = OnboardingSection.entries[it.step]) }
        }
    }

    private fun finish(save: Boolean) = launch("finishOnboarding") {
        if (save) preferencesRepository.save(mutableState.value.preferences)
        router.goBack()
    }

    private fun toggle(value: String) {
        mutableState.update { state ->
            val current = state.preferences
            val updated = when (state.section) {
                OnboardingSection.Diets -> current.copy(
                    diets = current.diets.flip(DietPreference.valueOf(value)),
                )
                OnboardingSection.Allergens -> current.copy(
                    allergens = current.allergens.flip(AllergenPreference.valueOf(value)),
                )
                OnboardingSection.Tastes -> current.copy(
                    tastes = current.tastes.flip(TastePreference.valueOf(value)),
                )
                OnboardingSection.Equipment -> current.copy(
                    equipment = current.equipment.flip(KitchenEquipment.valueOf(value)),
                )
            }
            state.copy(preferences = updated)
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(
            showIntro: Boolean,
        ): OnboardingViewModel
    }
}

private fun <T> Set<T>.flip(value: T): Set<T> = if (value in this) this - value else this + value

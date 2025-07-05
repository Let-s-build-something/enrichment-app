package ui.account

import androidx.lifecycle.viewModelScope
import components.pull_refresh.RefreshableViewModel.Companion.MINIMUM_REFRESH_DELAY
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.io.base.BaseResponse
import data.io.social.UserConfiguration
import data.io.social.UserPrivacy
import data.io.social.UserVisibility
import data.shared.SharedModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val accountDashboardModule = module {
    factory { AccountDashboardRepository(get()) }
    viewModelOf(::AccountDashboardModel)
}

/** Communication between the UI, the control layers, and control and data layers */
class AccountDashboardModel(
    private val repository: AccountDashboardRepository
): SharedModel() {

    private val _signOutResponse = MutableStateFlow(false)
    private val _privacyResponse = MutableStateFlow<BaseResponse<Any>?>(null)
    private val _visibilityResponse = MutableStateFlow<BaseResponse<Any>?>(null)
    private val _isLoading = MutableStateFlow(false)

    val isLoading = _isLoading.asStateFlow()

    /** Response to a sign out request */
    val signOutResponse = _signOutResponse.asStateFlow()

    /** Response to an ongoing request for change of privacy */
    val privacyResponse = _privacyResponse.asStateFlow()

    /** Response to an ongoing request for change of visibility */
    val visibilityResponse = _visibilityResponse.asStateFlow()

    /** Sets the theme of the app */
    fun updateTheme(choiceOrdinal: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val choice = ThemeChoice.entries[choiceOrdinal]
            sharedDataManager.localSettings.update {
                it?.copy(theme = choice)
            }
            settings.putString(SettingsKeys.KEY_THEME, choice.name)
        }
    }


    /** Sets the privacy setting of a user */
    fun requestPrivacyChange(privacy: UserPrivacy) {
        viewModelScope.launch {
            _privacyResponse.value = BaseResponse.Loading
            val startTime = Clock.System.now().toEpochMilliseconds()

            val newConfiguration = currentUser.value?.configuration?.copy(privacy = privacy)
                ?: UserConfiguration(privacy = privacy)

            _privacyResponse.value = repository.changeUserConfiguration(
                configuration = newConfiguration
            )
            if(_privacyResponse.value is BaseResponse.Success) {
                sharedDataManager.currentUser.update { previous ->
                    previous?.copy(configuration = newConfiguration)
                }
            }
            delay(kotlin.math.max(
                Clock.System.now().toEpochMilliseconds().minus(startTime),
                MINIMUM_REFRESH_DELAY
            ))
            _privacyResponse.value = null
        }
    }

    /** Sets the visibility setting of a user */
    fun requestVisibilityChange(visibility: UserVisibility) {
        viewModelScope.launch {
            _visibilityResponse.value = BaseResponse.Loading
            val startTime = Clock.System.now().toEpochMilliseconds()

            val newConfiguration = currentUser.value?.configuration?.copy(visibility = visibility)
                ?: UserConfiguration(visibility = visibility)

            _visibilityResponse.value = repository.changeUserConfiguration(
                configuration = newConfiguration
            )
            if(_visibilityResponse.value is BaseResponse.Success) {
                sharedDataManager.currentUser.update { previous ->
                    previous?.copy(configuration = newConfiguration)
                }
            }
            delay(kotlin.math.max(
                Clock.System.now().toEpochMilliseconds().minus(startTime),
                MINIMUM_REFRESH_DELAY
            ))
            _visibilityResponse.value = null
        }
    }

    /** Logs out the currently signed in user */
    override suspend fun logoutCurrentUser() {
        _isLoading.value = true
        super.logoutCurrentUser()
        _signOutResponse.emit(true)
        _isLoading.value = false
    }

    fun logout() {
        viewModelScope.launch {
            logoutCurrentUser()
        }
    }
}
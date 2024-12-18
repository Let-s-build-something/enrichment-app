@file:OptIn(ExperimentalSettingsApi::class)

package data.shared

import androidx.lifecycle.viewModelScope
import base.utils.asSimpleString
import com.russhwolf.settings.ExperimentalSettingsApi
import data.NetworkProximityCategory
import data.io.app.ClientStatus
import data.io.app.LocalSettings
import data.io.app.SettingsKeys
import data.io.app.ThemeChoice
import data.io.user.UserIO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.messaging.messaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val appServiceModule = module {
    single<AppServiceDataManager> { AppServiceDataManager() }
    factory { AppServiceViewModel(get()) }
    viewModelOf(::AppServiceViewModel)
}

class AppServiceDataManager {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = MutableSharedFlow<String>()
}

/** Shared viewmodel for services specific to the runtime of the application */
class AppServiceViewModel(private val dataManager: AppServiceDataManager): SharedViewModel() {

    /** Newly emitted deep link which should be handled */
    val newDeeplink = dataManager.newDeeplink.asSharedFlow()

    /** Whether leave dialog should be shown */
    var showLeaveDialog: Boolean = true


    /** Initializes the application */
    fun initApp() {
        CoroutineScope(Dispatchers.IO).launch {
            showLeaveDialog = settings.getBooleanOrNull(SettingsKeys.KEY_SHOW_LEAVE_DIALOG) ?: true

            if (sharedDataManager.localSettings.value == null) {
                val defaultFcm = settings.getStringOrNull(SettingsKeys.KEY_FCM)

                val fcmToken = if(defaultFcm == null) {
                    val newFcm = try {
                        Firebase.messaging.getToken()
                    }catch (e: NotImplementedError) { null }?.apply {
                        settings.putString(SettingsKeys.KEY_FCM, this)
                    }
                    newFcm
                }else defaultFcm

                sharedDataManager.localSettings.value = LocalSettings(
                    theme = ThemeChoice.entries.find {
                        it.name == settings.getStringOrNull(SettingsKeys.KEY_THEME)
                    } ?: ThemeChoice.SYSTEM,
                    fcmToken = fcmToken,
                    clientStatus = ClientStatus.entries.find {
                        it.name == settings.getStringOrNull(SettingsKeys.KEY_CLIENT_STATUS)
                    } ?: ClientStatus.NEW,
                    networkColors = settings.getStringOrNull(SettingsKeys.KEY_NETWORK_COLORS)?.split(",")
                        ?: NetworkProximityCategory.entries.map { it.color.asSimpleString() }
                )
            }
        }
    }

    /** Save settings for leave dialog */
    fun saveDialogSetting(showAgain: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.putBoolean(SettingsKeys.KEY_SHOW_LEAVE_DIALOG, showAgain)
        }
    }

    /** Emits a new deep link for handling */
    fun emitDeepLink(uri: String?) {
        println("emitDeepLink, path: $uri")

        if(uri == null) return
        viewModelScope.launch {
            dataManager.newDeeplink.emit(
                uri
                    .replace("""^\/""".toRegex(), "")
                    .replace("""\/$""".toRegex(), "")
                    .replace("augmy://", "")
            )
        }
    }
}
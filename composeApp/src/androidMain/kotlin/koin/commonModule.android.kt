package koin

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin


@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual val settings: FlowSettings
    get() {
        val context = getKoin().get<Context>()

        return DataStoreSettings(PreferenceDataStoreFactory.createWithPath(
            produceFile = { context.preferencesDataStoreFile("app_preferences").absolutePath.toPath() }
        ))
    }
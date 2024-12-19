package koin

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import okio.Path.Companion.toPath
import java.io.File

@OptIn(ExperimentalSettingsImplementation::class, ExperimentalSettingsApi::class)
actual val settings: FlowSettings
    get() = DataStoreSettings(PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            File(
                System.getProperty("user.dir")
                        + File.separator
                        + "datastore"
                        + File.separator + "app_preferences.preferences_pb"
            ).absolutePath.toPath()
        }
    ))
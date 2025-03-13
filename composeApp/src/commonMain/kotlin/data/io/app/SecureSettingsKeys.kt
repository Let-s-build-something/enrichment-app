package data.io.app

/** keys for settings stored locally on the device */
object SecureSettingsKeys {

    const val KEY_CREDENTIALS = "matrix_credentials"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_PICKLE_KEY = "pickle_key"
    const val KEY_USER_ID = "user_id"

    val persistentKeys = listOf(
        KEY_DEVICE_ID,
        KEY_PICKLE_KEY
    )
}
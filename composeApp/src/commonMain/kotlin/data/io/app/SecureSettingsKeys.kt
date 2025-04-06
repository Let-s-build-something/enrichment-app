package data.io.app

/** keys for settings stored locally on the device */
object SecureSettingsKeys {

    const val SECRET_BYTE_ARRAY_KEY_KEY = "secret_byte_array_key_key"
    const val KEY_CREDENTIALS = "key_matrix_credentials"
    const val KEY_DEVICE_ID = "key_device_id"
    const val KEY_DB_PASSWORD = "key_db_password"
    const val KEY_DB_KEY = "key_db_key"
    const val KEY_PICKLE_KEY = "key_pickle_key"
    const val KEY_USER_ID = "key_user_id"

    val persistentKeys = listOf(
        KEY_DEVICE_ID,
        KEY_DB_PASSWORD,
        KEY_DB_KEY,
        SECRET_BYTE_ARRAY_KEY_KEY,
        KEY_PICKLE_KEY
    )
}
package data.io.app

/** keys for settings stored locally on the device */
object SecureSettingsKeys {

    const val KEY_CREDENTIALS = "matrix_credentials"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_PICKLE_KEY = "device_id"
    const val KEY_OLM_ACCOUNT = "olm_account"
    const val KEY_FALLBACK_INSTANT = "fallback_instant"
    const val KEY_SECRETS = "secrets"
    const val KEY_CROSS_SIGNING_KEY = "cross_signing_key"
    const val KEY_SECRET_KEY_EVENT = "secret_key_event"

    val persistentKeys = listOf(
        KEY_DEVICE_ID,
        KEY_PICKLE_KEY,
        KEY_OLM_ACCOUNT,
        KEY_FALLBACK_INSTANT,
        KEY_SECRETS,
        KEY_CROSS_SIGNING_KEY,
        KEY_SECRET_KEY_EVENT
    )
}
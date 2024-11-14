package data.io.app

/** keys for settings stored locally on the device */
object SettingsKeys {

    /** key for the user-selected theme */
    const val KEY_THEME = "device_theme"

    /** key for the fcm token */
    const val KEY_FCM = "device_fcm"

    /** key for current client status */
    const val KEY_CLIENT_STATUS = "client_status"

    /** key for current last selected network categories */
    const val KEY_NETWORK_CATEGORIES = "network_categories"

    /** Whether toolbar is currently expanded or not */
    const val KEY_TOOLBAR_EXPANDED = "toolbar_expanded"

    /** Whether dialog when leaving should be displayed */
    const val SHOW_LEAVE_DIALOG = "show_leave_dialog"
}
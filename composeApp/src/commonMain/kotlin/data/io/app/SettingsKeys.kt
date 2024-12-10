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

    /** key for currently selected color preferences */
    const val KEY_NETWORK_COLORS = "network_colors"

    /** Whether toolbar is currently expanded or not */
    const val KEY_TOOLBAR_EXPANDED = "toolbar_expanded"

    /** Whether dialog when leaving should be displayed */
    const val KEY_SHOW_LEAVE_DIALOG = "show_leave_dialog"

    /** Prefix key for last unsent message */
    const val KEY_LAST_MESSAGE = "last_message"

    /** Height of soft keyboard */
    const val KEY_KEYBOARD_HEIGHT = "keyboard_height"
}
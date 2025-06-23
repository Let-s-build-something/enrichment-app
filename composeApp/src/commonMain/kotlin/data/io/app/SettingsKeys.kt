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

    const val KEY_REFEREE_USER_ID = "referee_user_id"
    const val KEY_REFERRER_FINISHED = "referrer_finished"

    /** Prefix key for last unsent message */
    const val KEY_LAST_MESSAGE = "last_message"
    const val KEY_LAST_MESSAGE_TIMINGS = "last_message_timings"
    const val KEY_LAST_MESSAGE_GRAVITY = "last_message_gravity"

    /** Height of soft keyboard */
    const val KEY_KEYBOARD_HEIGHT = "keyboard_height"

    /** Whether hint about emoji preferences should be displayed */
    const val KEY_SHOW_EMOJI_PREFERENCE_HINT = "show_emoji_preference_hint"

    /** List of preferred emojis */
    const val KEY_PREFERRED_EMOJIS = "preferred_emojis"

    const val KEY_PACING_WIDE_AVG = "pacing_wide_avg"
    const val KEY_PACING_NARROW_AVG = "pacing_narrow_avg"


    // DEV use only
    const val KEY_STREAMING_URL = "streaming_url"
    const val KEY_STREAMING_DIRECTORY = "streaming_directory"
}
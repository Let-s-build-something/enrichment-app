package ui.conversation.components

/** Different keyboard modes for the conversation */
enum class ConversationKeyboardMode {
    /** Emoji picker with filter */
    Emoji,

    /** GIF picker with search */
    Gif,

    /** Stickers picker with bundles */
    Stickers,

    /** The initial mode, regular soft keyboard with textual input */
    Default
}
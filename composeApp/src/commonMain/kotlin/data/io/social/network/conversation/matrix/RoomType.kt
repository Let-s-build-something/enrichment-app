package data.io.social.network.conversation.matrix

enum class RoomType {
    /** The rooms that the user has been invited to. */
    Invited,

    /** The rooms that the user has joined. */
    Joined,

    /** The rooms that the user has knocked upon. */
    Knocked,

    /** The rooms that the user has left or been banned from. */
    Left
}
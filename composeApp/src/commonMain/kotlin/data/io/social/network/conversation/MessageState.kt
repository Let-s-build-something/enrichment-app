package data.io.social.network.conversation

/** State of a message */
enum class MessageState {
    /** Message is being sent to the server */
    SENDING,

    /** Successfully uploaded to the server */
    SENT,

    /** Received by the other party, in other words, server attempted to notify the receiver */
    RECEIVED,

    /** Message was read by the recipient */
    READ,

    /** There was a problem with sending this message */
    FAILED
}
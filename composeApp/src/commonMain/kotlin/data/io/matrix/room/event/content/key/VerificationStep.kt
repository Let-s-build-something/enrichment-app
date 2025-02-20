package data.io.matrix.room.event.content.key

import data.io.matrix.room.event.content.MessageEventContent
import data.io.matrix.room.event.content.ToDeviceEventContent


interface VerificationStep : MessageEventContent, ToDeviceEventContent {
    val transactionId: String?
}
package data.shared

import dev.gitlive.firebase.storage.Data
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun fromByteArrayToData(byteArray: ByteArray): Data {
    return byteArray.usePinned { pinned ->
        Data(NSData.create(bytes = pinned.addressOf(0), length = byteArray.size.toULong()))
    }
}
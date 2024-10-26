package data.shared

import dev.gitlive.firebase.storage.Data

actual fun fromByteArrayToData(byteArray: ByteArray): Data = Data(byteArray)
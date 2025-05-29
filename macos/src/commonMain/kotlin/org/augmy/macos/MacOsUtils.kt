package org.augmy.macos

data class ForegroundApp(
    val localizedName: String?,
    val bundleIdentifier: String?
)

expect fun getForegroundApp(): ForegroundApp?

expect fun getBatteryLevel(): Double?

expect fun getMainDisplayBrightness(): Float?

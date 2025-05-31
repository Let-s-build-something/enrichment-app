package org.augmy.macos

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.AppKit.NSWorkspace
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFGetTypeID
import platform.CoreFoundation.CFNumberGetTypeID
import platform.CoreFoundation.CFNumberGetValue
import platform.CoreFoundation.CFNumberRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFNumberIntType
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreGraphics.CGDisplayIOServicePort
import platform.CoreGraphics.CGMainDisplayID
import platform.IOKit.IODisplayGetFloatParameter
import platform.IOKit.IOPSCopyPowerSourcesInfo
import platform.IOKit.IOPSCopyPowerSourcesList
import platform.IOKit.IOPSGetPowerSourceDescription
import platform.IOKit.kIOReturnSuccess



actual fun getForegroundApp(): ForegroundApp? {
    val workspace = NSWorkspace.sharedWorkspace()
    val activeApp = workspace.frontmostApplication ?: return null
    return ForegroundApp(
        bundleIdentifier = activeApp.bundleIdentifier,
        localizedName = activeApp.localizedName
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun getMainDisplayBrightness(): Float? = memScoped {
    val displayId = CGMainDisplayID()
    val service = CGDisplayIOServicePort(displayId)

    val brightnessPtr = alloc<FloatVar>()
    val brightnessKey = CFStringCreateWithCString(null, "brightness", kCFStringEncodingUTF8)

    val result = IODisplayGetFloatParameter(service, 0u, brightnessKey, brightnessPtr.ptr)

    return if (result == kIOReturnSuccess) brightnessPtr.value else null
}

@OptIn(ExperimentalForeignApi::class)
actual fun getBatteryLevel(): Double? {
    val blob = IOPSCopyPowerSourcesInfo() ?: return -1.0
    val list = IOPSCopyPowerSourcesList(blob) ?: return -1.0

    val count = CFArrayGetCount(list)
    if (count == 0L) return -1.0

    val ps = CFArrayGetValueAtIndex(list, 0) ?: return -1.0
    val description = IOPSGetPowerSourceDescription(blob, ps) ?: return -1.0

    val currentCapacity = description.getCFInt("Current Capacity") ?: return -1.0
    val maxCapacity = description.getCFInt("Max Capacity") ?: return -1.0

    return currentCapacity.toDouble() / maxCapacity.toDouble()
}

@OptIn(ExperimentalForeignApi::class)
fun CFDictionaryRef.getCFInt(key: String): Int? {
    val cfKey = key.toCFString()
    val valueRef = CFDictionaryGetValue(this, cfKey)
    if (valueRef != null && CFGetTypeID(valueRef) == CFNumberGetTypeID()) {
        val number = valueRef as? CFNumberRef
        val intPtr = nativeHeap.alloc<IntVar>()
        val success = CFNumberGetValue(number, kCFNumberIntType, intPtr.ptr)
        return if (success) intPtr.value else null
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
fun String.toCFString(): CFStringRef? = CFStringCreateWithCString(null, this, kCFStringEncodingUTF8)

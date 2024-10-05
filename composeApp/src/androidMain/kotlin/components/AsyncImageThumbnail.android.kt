package components

import augmy.interactive.com.AndroidApp
import coil3.PlatformContext

actual val platformContext: PlatformContext
    get() = AndroidApp.instance
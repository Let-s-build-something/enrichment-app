package base

import dev.gitlive.firebase.Firebase
import io.ktor.util.Platform

/** Platform using this application */
actual val currentPlatform: Platform = Platform.Jvm

/** Platform specific Firebase instance */
actual val PlatformFirebase: Firebase? = null
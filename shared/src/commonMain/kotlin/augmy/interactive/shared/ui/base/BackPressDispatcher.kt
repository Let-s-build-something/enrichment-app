package augmy.interactive.shared.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle

/** Interface for communicating with platform specific back press dispatcher */
interface BackPressDispatcher {
    /** Called whenever user attempts to navigate up */
    fun addOnBackPressedListener(listener: () -> Unit)

    /** Attempt to remove a listener */
    fun removeOnBackPressedListener(listener: () -> Unit)

    /** Called whenever system attempts to navigate up */
    fun executeSystemBackPress()

    fun executeBackPress()
}

/** Creates an on back press handler. */
@Composable
fun OnBackHandler(enabled: Boolean = true, onBack: () -> Unit) {
    val dispatcher = LocalBackPressDispatcher.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    if (lifecycleOwner.lifecycle.currentState === Lifecycle.State.DESTROYED) {
        return
    }

    val callback = remember(onBack) {
        {
            onBack()
        }
    }

    LaunchedEffect(dispatcher, onBack, callback, enabled) {
        if(enabled) {
            dispatcher?.addOnBackPressedListener(callback)
        }else dispatcher?.removeOnBackPressedListener(callback)
    }

    DisposableEffect(lifecycleOwner, callback) {
        onDispose {
            dispatcher?.removeOnBackPressedListener(callback)
        }
    }
}

@Composable
expect fun BackHandlerOverride(onBack: () -> Unit)

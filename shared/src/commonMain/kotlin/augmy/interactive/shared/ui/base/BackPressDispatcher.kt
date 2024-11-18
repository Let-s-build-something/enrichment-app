package augmy.interactive.shared.ui.base

/** Interface for communicating with platform specific back press dispatcher */
interface BackPressDispatcher {
    /** Called whenever user attempts to navigate up */
    fun addOnBackPressedListener(listener: () -> Unit)

    /** Called whenever system attempts to navigate up */
    fun executeBackPress()
}
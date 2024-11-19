
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.ComposeUIViewController
import augmy.interactive.shared.ui.base.BackPressDispatcher
import augmy.interactive.shared.ui.base.LocalBackPressDispatcher
import augmy.interactive.shared.ui.base.LocalScreenSize
import platform.UIKit.UIApplication
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.navigationController
import platform.darwin.NSObject

@OptIn(ExperimentalComposeUiApi::class)
fun MainViewController() = ComposeUIViewController {
    val containerSize = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current

    val backPressDispatcher = object: BackPressDispatcher {
        var listener: (() -> Unit)? = null

        override fun addOnBackPressedListener(listener: () -> Unit) {
            this.listener = listener
        }

        override fun executeBackPress() {
            UIApplication.sharedApplication.keyWindow
                ?.rootViewController
                ?.navigationController
                ?.popViewControllerAnimated(true)
        }
    }

    UIApplication.sharedApplication.keyWindow
        ?.rootViewController
        ?.navigationController
        ?.delegate = object : NSObject(), UINavigationControllerDelegateProtocol {
        override fun navigationController(
            navigationController: platform.UIKit.UINavigationController,
            willShowViewController: UIViewController,
            animated: Boolean
        ) {
            backPressDispatcher.listener?.invoke()
        }
    }

    CompositionLocalProvider(
        LocalBackPressDispatcher provides backPressDispatcher,
        LocalScreenSize provides IntSize(
            height = with(density) { containerSize.height.toDp() }.value.toInt(),
            width = with(density) { containerSize.width.toDp() }.value.toInt()
        )
    ) {
        App()
    }
}

// Extension function to find the top view controller
fun UIViewController.getTopViewController(): UIViewController? {
    var currentController: UIViewController? = this
    while (currentController?.presentedViewController != null) {
        currentController = currentController.presentedViewController
    }
    return currentController
}
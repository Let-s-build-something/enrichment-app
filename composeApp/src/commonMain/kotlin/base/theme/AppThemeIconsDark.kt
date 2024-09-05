package base.theme

import chatenrichment.composeapp.generated.resources.Res
import chatenrichment.composeapp.generated.resources.logo_apple_light
import chatenrichment.composeapp.generated.resources.logo_google_dark
import future_shared_module.theme.ThemeIcons
import org.jetbrains.compose.resources.DrawableResource

/** icons specific to main app theme [LocalTheme.current] */
object AppThemeIconsDark: ThemeIcons {

    override val googleSignUp: DrawableResource = Res.drawable.logo_google_dark
    override val appleSignUp: DrawableResource = Res.drawable.logo_apple_light
}
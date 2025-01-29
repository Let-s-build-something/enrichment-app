package base.theme

import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.logo_apple_light
import augmy.composeapp.generated.resources.logo_giphy_dark
import augmy.composeapp.generated.resources.logo_google_dark
import augmy.composeapp.generated.resources.logo_matrix_dark
import augmy.interactive.shared.ui.theme.ThemeIcons
import org.jetbrains.compose.resources.DrawableResource

/** icons specific to main app theme LocalTheme.current */
object AppThemeIconsDark: ThemeIcons {
    override val googleSignUp: DrawableResource = Res.drawable.logo_google_dark
    override val appleSignUp: DrawableResource = Res.drawable.logo_apple_light
    override val matrixSignUp: DrawableResource = Res.drawable.logo_matrix_dark
    override val giphy: DrawableResource = Res.drawable.logo_giphy_dark
}
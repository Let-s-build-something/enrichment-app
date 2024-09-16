@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package chatenrichment.shared.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi

private object CommonMainDrawable0 {
  public val logo_apple_dark: DrawableResource by 
      lazy { init_logo_apple_dark() }

  public val logo_apple_light: DrawableResource by 
      lazy { init_logo_apple_light() }

  public val logo_google_dark: DrawableResource by 
      lazy { init_logo_google_dark() }

  public val logo_google_light: DrawableResource by 
      lazy { init_logo_google_light() }
}

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("logo_apple_dark", CommonMainDrawable0.logo_apple_dark)
  map.put("logo_apple_light", CommonMainDrawable0.logo_apple_light)
  map.put("logo_google_dark", CommonMainDrawable0.logo_google_dark)
  map.put("logo_google_light", CommonMainDrawable0.logo_google_light)
}

internal val Res.drawable.logo_apple_dark: DrawableResource
  get() = CommonMainDrawable0.logo_apple_dark

private fun init_logo_apple_dark(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:logo_apple_dark",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/chatenrichment.shared.generated.resources/drawable/logo_apple_dark.xml", -1, -1),
    )
)

internal val Res.drawable.logo_apple_light: DrawableResource
  get() = CommonMainDrawable0.logo_apple_light

private fun init_logo_apple_light(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:logo_apple_light",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/chatenrichment.shared.generated.resources/drawable/logo_apple_light.xml", -1, -1),
    )
)

internal val Res.drawable.logo_google_dark: DrawableResource
  get() = CommonMainDrawable0.logo_google_dark

private fun init_logo_google_dark(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:logo_google_dark",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/chatenrichment.shared.generated.resources/drawable/logo_google_dark.xml", -1, -1),
    )
)

internal val Res.drawable.logo_google_light: DrawableResource
  get() = CommonMainDrawable0.logo_google_light

private fun init_logo_google_light(): DrawableResource =
    org.jetbrains.compose.resources.DrawableResource(
  "drawable:logo_google_light",
    setOf(
      org.jetbrains.compose.resources.ResourceItem(setOf(),
    "composeResources/chatenrichment.shared.generated.resources/drawable/logo_google_light.xml", -1, -1),
    )
)

package ui.login.username

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_username_change_launcher_animation
import augmy.composeapp.generated.resources.username_change_launcher_cancel
import augmy.composeapp.generated.resources.username_change_launcher_confirm
import augmy.composeapp.generated.resources.username_change_launcher_content
import augmy.composeapp.generated.resources.username_change_launcher_hint
import augmy.composeapp.generated.resources.username_change_launcher_title
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import koin.usernameChangeModule
import augmy.interactive.shared.ui.theme.LocalTheme
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.context.loadKoinModules

/**
 * Bottom sheet for notifying user about username change and enforcing it
 */
@OptIn(ExperimentalMaterial3Api::class, KoinExperimentalAPI::class, ExperimentalResourceApi::class)
@Composable
fun UsernameChangeLauncher(
    modifier: Modifier = Modifier,
    viewModel: UsernameChangeViewModel = koinViewModel(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    loadKoinModules(usernameChangeModule)

    val currentUser = viewModel.firebaseUser.collectAsState(null)

    val nameOutput = rememberSaveable {
        mutableStateOf(currentUser.value?.displayName ?: "")
    }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/anim.json").decodeToString()
        )
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = Compottie.IterateForever
                ),
                contentDescription = stringResource(Res.string.accessibility_username_change_launcher_animation)
            )
            Column(
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = stringResource(Res.string.username_change_launcher_title),
                    style = LocalTheme.current.styles.category
                )
                Text(
                    text = stringResource(Res.string.username_change_launcher_content),
                    style = LocalTheme.current.styles.regular
                )
            }
        }

        EditFieldInput(
            modifier = Modifier.padding(top = 12.dp),
            hint = stringResource(Res.string.username_change_launcher_hint),
            value = currentUser.value?.displayName ?: "",
            onValueChange = { value ->
                nameOutput.value = value
            },
            paddingValues = PaddingValues(start = 16.dp)
        )

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ErrorHeaderButton(
                text = stringResource(Res.string.username_change_launcher_cancel),
                endIconVector = Icons.AutoMirrored.Outlined.Logout,
                onClick = {
                    viewModel.logoutCurrentUser()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            BrandHeaderButton(
                text = stringResource(Res.string.username_change_launcher_confirm),
                isEnabled = nameOutput.value.length >= MIN_NAME_LENGTH,
                onClick = {
                    viewModel.saveName(nameOutput.value)
                }
            )
        }
    }
}

private const val MIN_NAME_LENGTH = 6
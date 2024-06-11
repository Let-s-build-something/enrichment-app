package lets.build.chatenrichment.ui.login.username

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.squadris.squadris.compose.components.SimpleModalBottomSheet
import com.squadris.squadris.compose.theme.LocalTheme
import lets.build.chatenrichment.R
import lets.build.chatenrichment.ui.components.BrandHeaderButton
import lets.build.chatenrichment.ui.components.EditFieldInput
import lets.build.chatenrichment.ui.components.ErrorHeaderButton

/**
 * Bottom sheet for notifying user about username change and enforcing it
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsernameChangeLauncher(
    modifier: Modifier = Modifier,
    viewModel: UsernameChangeViewModel = hiltViewModel(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val currentUser = viewModel.currentUser.collectAsState()

    val nameOutput = rememberSaveable {
        mutableStateOf(currentUser.value?.displayName ?: "")
    }

    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.anim_wave)
    )

    SimpleModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Start
        ) {
            LottieAnimation(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(64.dp),
                composition = lottieComposition,
                restartOnPlay = true,
                isPlaying = true,
                contentScale = ContentScale.FillHeight,
                iterations = Int.MAX_VALUE
            )
            Column(
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = stringResource(R.string.username_change_launcher_title),
                    style = LocalTheme.current.styles.category
                )
                Text(
                    text = stringResource(R.string.username_change_launcher_content),
                    style = LocalTheme.current.styles.regular
                )
            }
        }

        EditFieldInput(
            modifier = Modifier.padding(top = 12.dp),
            hint = stringResource(R.string.username_change_launcher_hint),
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
                text = stringResource(R.string.username_change_launcher_cancel),
                endIconVector = Icons.AutoMirrored.Outlined.Logout,
                onClick = {
                    viewModel.logoutCurrentUser()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            BrandHeaderButton(
                text = stringResource(R.string.username_change_launcher_confirm),
                isEnabled = nameOutput.value.length >= MIN_NAME_LENGTH,
                onClick = {
                    viewModel.saveName(nameOutput.value)
                }
            )
        }
    }
}

private const val MIN_NAME_LENGTH = 6
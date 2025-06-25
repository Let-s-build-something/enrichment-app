package ui.conversation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_upload_files
import augmy.composeapp.generated.resources.accessibility_upload_url
import augmy.composeapp.generated.resources.account_picture_change_success
import augmy.composeapp.generated.resources.account_picture_pick_title
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.error_permission
import augmy.composeapp.generated.resources.image_field_url_error_formats
import augmy.composeapp.generated.resources.image_field_url_hint
import augmy.composeapp.generated.resources.username_change_launcher_confirm
import augmy.interactive.shared.ui.base.CustomSnackbarVisuals
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.dialog.DialogShell
import augmy.interactive.shared.ui.components.input.CustomTextField
import augmy.interactive.shared.ui.theme.LocalTheme
import base.global.verification.ClickableTile
import data.io.base.BaseResponse
import data.io.matrix.room.FullConversationRoom
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import ui.conversation.components.MediaElement

@Composable
fun DialogChangeRoomAvatar(
    detail: FullConversationRoom?,
    model: ConversationSettingsModel,
    onDismissRequest: () -> Unit
) {
    val snackbarHostState = LocalSnackbarHost.current
    val focusManager = LocalFocusManager.current

    val ongoingChange = model.ongoingChange.collectAsState()

    val urlState = remember { TextFieldState() }
    val selectedImageUrl = rememberSaveable(detail?.data?.id) {
        mutableStateOf(
            urlState.text.ifEmpty { detail?.data?.summary?.avatar?.url }
        )
    }
    val selectedFile = remember { mutableStateOf<PlatformFile?>(null) }
    val isUrlInEdit = rememberSaveable { mutableStateOf(false) }
    val urlLoadState = remember {
        mutableStateOf<BaseResponse<*>?>(null)
    }

    LaunchedEffect(urlState.text) {
        if(urlState.text.isEmpty()) urlLoadState.value = null
    }

    val launcher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        mode = FileKitMode.Single,
        title = stringResource(Res.string.account_picture_pick_title)
    ) { file ->
        if(file != null) {
            selectedFile.value = file
            selectedImageUrl.value = detail?.data?.summary?.avatar?.url
        }
    }

    LaunchedEffect(Unit) {
        model.ongoingChange.collectLatest { change ->
            if(change?.state is BaseResponse.Error) {
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState?.showSnackbar(
                        CustomSnackbarVisuals(
                            message = getString(Res.string.error_permission),
                            isError = true
                        )
                    )
                }
                onDismissRequest()
            }else if (change?.state is BaseResponse.Success) {
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState?.showSnackbar(getString(Res.string.account_picture_change_success))
                }
                onDismissRequest()
            }
        }
    }

    DialogShell(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = {
                focusManager.clearFocus()
                if(urlState.text.isBlank()) isUrlInEdit.value = false
            })
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MediaElement(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(.4f)
                        .widthIn(max = 250.dp)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(
                            color = LocalTheme.current.colors.brandMain,
                            shape = CircleShape
                        ),
                    media = detail?.data?.summary?.avatar?.copy(url = selectedImageUrl.value.toString()),
                    contentScale = ContentScale.Crop,
                    localMedia = selectedFile.value,
                    onState = { loadState ->
                        if(isUrlInEdit.value) {
                            if(loadState is BaseResponse.Success) {
                                selectedImageUrl.value = urlState.text
                                isUrlInEdit.value = false
                            }else urlLoadState.value = loadState
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClickableTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Upload,
                        text = stringResource(Res.string.accessibility_upload_files),
                        onClick = {
                            launcher.launch()
                        },
                        isSelected = selectedFile.value != null && !isUrlInEdit.value
                    )
                    ClickableTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Link,
                        text = stringResource(Res.string.accessibility_upload_url),
                        onClick = {
                            isUrlInEdit.value = true
                        },
                        isSelected = isUrlInEdit.value
                    )
                }
                androidx.compose.animation.AnimatedVisibility(isUrlInEdit.value) {
                    CustomTextField(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxWidth(),
                        hint = stringResource(Res.string.image_field_url_hint),
                        prefixIcon = Icons.Outlined.Link,
                        state = urlState,
                        lineLimits = TextFieldLineLimits.SingleLine,
                        errorText = if(urlLoadState.value is BaseResponse.Error) {
                            stringResource(Res.string.image_field_url_error_formats)
                        }else null,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        trailingIcon = if(urlLoadState.value is BaseResponse.Loading) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.requiredSize(24.dp),
                                    color = LocalTheme.current.colors.brandMainDark,
                                    trackColor = LocalTheme.current.colors.tetrial
                                )
                            }
                        }else null,
                        isClearable = true
                    )
                }
                Row(
                    modifier = Modifier.padding(
                        bottom = 16.dp,
                        top = 8.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ErrorHeaderButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(Res.string.button_dismiss),
                        onClick = onDismissRequest
                    )
                    BrandHeaderButton(
                        modifier = Modifier.weight(1f),
                        isLoading = ongoingChange.value?.state is BaseResponse.Loading,
                        text = stringResource(Res.string.username_change_launcher_confirm),
                        isEnabled = selectedImageUrl.value != detail?.data?.summary?.avatar?.url
                                || (selectedFile.value != null && selectedFile.value?.name != detail?.data?.summary?.avatar?.name),
                        onClick = {
                            model.requestAvatarChange(
                                selectedFile.value,
                                selectedImageUrl.value?.toString()
                            )
                        }
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}
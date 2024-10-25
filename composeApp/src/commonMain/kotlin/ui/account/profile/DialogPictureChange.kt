package ui.account.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.accessibility_upload_files
import augmy.composeapp.generated.resources.accessibility_upload_url
import augmy.composeapp.generated.resources.account_picture_change_success
import augmy.composeapp.generated.resources.account_picture_custom
import augmy.composeapp.generated.resources.account_picture_peeps
import augmy.composeapp.generated.resources.account_picture_pick_title
import augmy.composeapp.generated.resources.button_dismiss
import augmy.composeapp.generated.resources.username_change_launcher_confirm
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.ErrorHeaderButton
import augmy.interactive.shared.ui.components.dialog.DialogShell
import augmy.interactive.shared.ui.theme.LocalTheme
import coil3.compose.AsyncImage
import components.AsyncSvgImage
import data.Asset
import future_shared_module.ext.scalingClickable
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.ktor.http.headers
import koin.profileChangeModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules

@Composable
fun DialogPictureChange(onDismissRequest: () -> Unit) {
    loadKoinModules(profileChangeModule)
    val viewModel: ProfileChangeViewModel = koinViewModel()
    val snackbarHostState = LocalSnackbarHost.current
    val coroutineScope = rememberCoroutineScope()

    val firebaseUser = viewModel.firebaseUser.collectAsState()
    val isLoading = viewModel.isLoading.collectAsState()

    val selectedImageUrl = rememberSaveable {
        mutableStateOf(try { firebaseUser.value?.photoURL }catch (e: NotImplementedError) { null } ?: "")
    }

    val launcher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = stringResource(Res.string.account_picture_pick_title)
    ) { file ->
        if(file != null) {
            coroutineScope.launch {
                viewModel.requestPictureUpload(
                    mediaByteArray = file.readBytes(),
                    fileName = file.name
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.isPictureChangeSuccess.collectLatest { res ->
            if(res == true) {
                CoroutineScope(Dispatchers.Main).launch {
                    snackbarHostState?.showSnackbar(
                        message = getString(Res.string.account_picture_change_success)
                    )
                }
                onDismissRequest()
            }
        }
    }

    DialogShell(
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
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
                    model = selectedImageUrl.value,
                    contentDescription = null
                )
                Row(
                    modifier = Modifier
                        .padding(
                            bottom = 16.dp,
                            top = 8.dp,
                            start = 16.dp,
                            end = 16.dp
                        )
                        .fillMaxWidth(),
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
                        isLoading = isLoading.value,
                        text = stringResource(Res.string.username_change_launcher_confirm),
                        isEnabled = selectedImageUrl.value != try { firebaseUser.value?.photoURL }catch (e: NotImplementedError) { null },
                        onClick = {
                            viewModel.requestPictureChange(selectedImageUrl.value)
                        }
                    )
                }
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(.5f)
                        .background(
                            color = LocalTheme.current.colors.tetrial,
                            shape = RoundedCornerShape(
                                topStart = LocalTheme.current.shapes.componentCornerRadius,
                                topEnd = LocalTheme.current.shapes.componentCornerRadius
                            )
                        ),
                    columns = GridCells.Adaptive(minSize = 100.dp)
                ) {
                    headers {
                        item(span = {
                            GridItemSpan(maxCurrentLineSpan)
                        }) {
                            Column {
                                Text(
                                    modifier = Modifier
                                        .padding(top = 16.dp)
                                        .fillMaxWidth(),
                                    text = stringResource(Res.string.account_picture_custom),
                                    style = LocalTheme.current.styles.category.copy(
                                        textAlign = TextAlign.Center,
                                        color = LocalTheme.current.colors.brandMainDark
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .fillMaxWidth()
                                        .background(
                                            color = LocalTheme.current.colors.tetrial,
                                            shape = LocalTheme.current.shapes.componentShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = LocalTheme.current.colors.brandMainDark,
                                            shape = LocalTheme.current.shapes.componentShape
                                        )
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .scalingClickable(
                                                onTap = {
                                                    launcher.launch()
                                                }
                                            ),
                                        imageVector = Icons.Outlined.Upload,
                                        contentDescription = stringResource(Res.string.accessibility_upload_files),
                                        tint = LocalTheme.current.colors.brandMainDark
                                    )
                                    Icon(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .scalingClickable(
                                                onTap = {

                                                }
                                            ),
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = stringResource(Res.string.accessibility_upload_url),
                                        tint = LocalTheme.current.colors.brandMainDark
                                    )
                                }
                            }
                        }
                    }
                    item(span = {
                        GridItemSpan(maxCurrentLineSpan)
                    }) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(Res.string.account_picture_peeps),
                            style = LocalTheme.current.styles.category.copy(
                                textAlign = TextAlign.Center,
                                color = LocalTheme.current.colors.brandMainDark
                            )
                        )
                    }
                    items(count = 105) { index ->
                        val peep = Asset.Peep(index)

                        AsyncSvgImage(
                            modifier = Modifier
                                .padding(1.5.dp)
                                .background(
                                    color = LocalTheme.current.colors.brandMain,
                                    shape = LocalTheme.current.shapes.rectangularActionShape
                                )
                                .scalingClickable(
                                    onTap = {
                                        selectedImageUrl.value = peep.url
                                    }
                                )
                                .heightIn(min = 100.dp)
                                .fillMaxSize()
                                .padding(8.dp),
                            asset = peep,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}
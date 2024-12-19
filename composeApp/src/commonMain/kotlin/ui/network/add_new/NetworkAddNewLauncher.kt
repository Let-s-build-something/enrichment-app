package ui.network.add_new

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.account_username_error_format
import augmy.composeapp.generated.resources.account_username_hint
import augmy.composeapp.generated.resources.error_general
import augmy.composeapp.generated.resources.network_inclusion_action
import augmy.composeapp.generated.resources.network_inclusion_description
import augmy.composeapp.generated.resources.network_inclusion_error_blocked
import augmy.composeapp.generated.resources.network_inclusion_error_duplicate
import augmy.composeapp.generated.resources.network_inclusion_error_non_existent
import augmy.composeapp.generated.resources.network_inclusion_error_non_format
import augmy.composeapp.generated.resources.network_inclusion_format_tag
import augmy.composeapp.generated.resources.network_inclusion_hint_tag
import augmy.composeapp.generated.resources.network_inclusion_proximity_title
import augmy.composeapp.generated.resources.network_inclusion_success
import augmy.composeapp.generated.resources.network_inclusion_success_action
import augmy.composeapp.generated.resources.screen_network_new
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.components.BrandHeaderButton
import augmy.interactive.shared.ui.components.SimpleModalBottomSheet
import augmy.interactive.shared.ui.components.input.EditFieldInput
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationArguments
import base.navigation.NavigationNode
import components.AsyncSvgImage
import data.NetworkProximityCategory
import data.io.ApiErrorCode
import augmy.interactive.shared.ext.scalingClickable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.context.loadKoinModules
import ui.account.profile.USERNAME_MAX_LENGTH
import ui.account.profile.USERNAME_MIN_LENGTH

private const val REGEX_USER_TAG = """^[a-fA-F0-9]+${'$'}"""

/** Screen for including new users to one's social network */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkAddNewLauncher(
    modifier: Modifier = Modifier,
    displayName: String? = null,
    tag: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest: () -> Unit
) {
    loadKoinModules(networkAddNewModule)
    val viewModel: NetworkAddNewViewModel = koinViewModel()

    val snackbarHostState = LocalSnackbarHost.current
    val navController = LocalNavController.current
    val isLoading = viewModel.isLoading.collectAsState()
    val customColors = viewModel.customColors.collectAsState(initial = hashMapOf())
    val recommendedUsers = viewModel.recommendedUsers.collectAsState(initial = hashMapOf())

    val inputDisplayName = rememberSaveable {
        mutableStateOf(displayName ?: "")
    }
    val inputTag = rememberSaveable {
        mutableStateOf(tag ?: "")
    }
    val errorMessage = remember {
        mutableStateOf<String?>(null)
    }
    val selectedCategory = remember {
        mutableStateOf(NetworkProximityCategory.Public)
    }
    val showProximityChoice = remember {
        mutableStateOf(false)
    }
    val focusRequester = remember { FocusRequester() }

    val isDisplayNameValid = with(inputDisplayName.value) {
        length in USERNAME_MIN_LENGTH..USERNAME_MAX_LENGTH
                && !startsWith(' ') && !endsWith(' ')
    }
    val isTagValid = with(inputTag.value) {
        length == 6 && REGEX_USER_TAG.toRegex().matches(this)
    }

    LaunchedEffect(isDisplayNameValid, isTagValid) {
        if(isDisplayNameValid && isTagValid && errorMessage.value == null) {
            showProximityChoice.value = true
        }
    }

    LaunchedEffect(Unit) {
        delay(400)
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        viewModel.response.collectLatest {
            it?.error?.let { error ->
                errorMessage.value = getString(
                    when(error.errors.firstOrNull()) {
                        ApiErrorCode.DUPLICATE.name -> Res.string.network_inclusion_error_duplicate
                        ApiErrorCode.NON_EXISTENT.name -> Res.string.network_inclusion_error_non_existent
                        ApiErrorCode.INVALID_FORMAT.name -> Res.string.network_inclusion_error_non_format
                        ApiErrorCode.USER_BLOCKED.name -> Res.string.network_inclusion_error_blocked
                        else -> Res.string.error_general
                    }
                )
            }
            it?.success?.data?.let { data ->
                val name = inputDisplayName.value + ""
                CoroutineScope(Dispatchers.Main).launch {
                    if(snackbarHostState?.showSnackbar(
                            message = getString(Res.string.network_inclusion_success),
                            actionLabel = if(data.isInclusionImmediate == true && data.targetPublicId != null) {
                                getString(Res.string.network_inclusion_success_action)
                            }else null,
                            duration = SnackbarDuration.Short
                        ) == SnackbarResult.ActionPerformed
                    ) {
                        onDismissRequest()
                        navController?.navigate(
                            NavigationNode.Conversation(
                                conversationId = data.targetPublicId,
                                name = name
                            )
                        )
                    }
                }
                navController?.previousBackStackEntry
                    ?.savedStateHandle
                    ?.apply {
                        set(NavigationArguments.NETWORK_NEW_SUCCESS, true)
                    }
                onDismissRequest()
            }
        }
    }

    SimpleModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.screen_network_new),
            style = LocalTheme.current.styles.subheading
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(Res.string.network_inclusion_description),
            style = LocalTheme.current.styles.regular
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EditFieldInput(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .weight(1f, fill = true),
                hint = stringResource(Res.string.account_username_hint),
                value = displayName ?: "",
                suggestText = if(!isDisplayNameValid) {
                    stringResource(Res.string.account_username_error_format)
                } else null,
                maxCharacters = USERNAME_MAX_LENGTH,
                errorText = errorMessage.value,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                onValueChange = { value ->
                    errorMessage.value = null
                    inputDisplayName.value = value.text
                }
            )
            EditFieldInput(
                modifier = Modifier.weight(.75f),
                hint = stringResource(Res.string.network_inclusion_hint_tag),
                value = tag ?: "",
                maxCharacters = 6,
                errorText = if(!isTagValid && inputTag.value.isNotBlank()) {
                    stringResource(Res.string.network_inclusion_format_tag)
                } else null,
                suggestText = if(inputTag.value.isBlank()) {
                    stringResource(Res.string.network_inclusion_format_tag)
                } else null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if(isDisplayNameValid && isTagValid) {
                            viewModel.includeNewUser(
                                displayName = inputDisplayName.value,
                                tag = inputTag.value,
                                proximity = selectedCategory.value
                            )
                        }
                    }
                ),
                leadingIcon = Icons.Outlined.Tag,
                onValueChange = { value ->
                    inputTag.value = value.text
                }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showProximityChoice.value,
            enter = expandVertically() + fadeIn()
        ) {
            Column {
                Text(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                    text = stringResource(Res.string.network_inclusion_proximity_title),
                    style = LocalTheme.current.styles.category
                )
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                ) {
                    val corner = LocalTheme.current.shapes.componentCornerRadius
                    NetworkProximityCategory.entries.forEachIndexed { index, category ->
                        val weight = animateFloatAsState(
                            targetValue = if(selectedCategory.value == category) 3f else 1f,
                            label = "weightChange"
                        )
                        val colorAlpha = animateFloatAsState(
                            targetValue = if(selectedCategory.value == category) 1f else .7f,
                            label = "alphaChange"
                        )

                        Column(
                            modifier = Modifier
                                .background(
                                    color = (customColors.value[category] ?: category.color).copy(colorAlpha.value),
                                    shape = RoundedCornerShape(
                                        bottomStart = if(index == 0) corner else 0.dp,
                                        topStart= if(index == 0) corner else 0.dp,
                                        bottomEnd = if(index == NetworkProximityCategory.entries.lastIndex) corner else 0.dp,
                                        topEnd = if(index == NetworkProximityCategory.entries.lastIndex) corner else 0.dp
                                    )
                                )
                                .weight(weight.value)
                                .fillMaxHeight()
                                .animateContentSize()
                                .scalingClickable(scaleInto = .95f) {
                                    selectedCategory.value = category
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                text = stringResource(category.res),
                                style = LocalTheme.current.styles.category.copy(
                                    color = Color.White.copy(colorAlpha.value),
                                    textAlign = TextAlign.Center
                                ),
                                maxLines = 1
                            )
                            recommendedUsers.value?.get(category)?.let { users ->
                                users.forEach { user ->
                                    Row(
                                        modifier = Modifier.padding(LocalTheme.current.shapes.betweenItemsSpace / 2),
                                        horizontalArrangement = Arrangement.spacedBy(LocalTheme.current.shapes.betweenItemsSpace),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncSvgImage(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .size(42.dp),
                                            model = user.photoUrl,
                                            contentDescription = null
                                        )
                                        if(selectedCategory.value == category) {
                                            Text(
                                                modifier = Modifier.weight(1f),
                                                text = user.displayName ?: "",
                                                style = LocalTheme.current.styles.category,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        BrandHeaderButton(
            modifier = Modifier.padding(top = 16.dp),
            isEnabled = isDisplayNameValid && isTagValid && errorMessage.value == null,
            isLoading = isLoading.value,
            text = stringResource(Res.string.network_inclusion_action),
            onClick = {
                viewModel.includeNewUser(
                    displayName = inputDisplayName.value,
                    tag = inputTag.value,
                    proximity = selectedCategory.value
                )
            }
        )
    }
}
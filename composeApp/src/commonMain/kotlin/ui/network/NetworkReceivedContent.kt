package ui.network

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paging.compose.collectAsLazyPagingItems
import augmy.composeapp.generated.resources.Res
import augmy.composeapp.generated.resources.network_request_accepted
import augmy.interactive.shared.ui.base.LocalNavController
import augmy.interactive.shared.ui.base.LocalSnackbarHost
import augmy.interactive.shared.ui.theme.LocalTheme
import base.navigation.NavigationArguments
import collectResult
import components.network.CircleSentRequestRow
import components.pull_refresh.RefreshableContent
import components.pull_refresh.RefreshableViewModel.Companion.requestData
import data.io.base.BaseResponse
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
fun NetworkReceivedContent(viewModel: NetworkReceivedViewModel = koinViewModel()) {
    val requests = viewModel.requests.collectAsLazyPagingItems()
    val response = viewModel.response.collectAsState()
    val isRefreshing = viewModel.isRefreshing.collectAsState()

    val snackbarHostState = LocalSnackbarHost.current
    val navController = LocalNavController.current

    LaunchedEffect(Unit) {
        viewModel.response.collectLatest { res ->
            if(res.any { it.value is BaseResponse.Success }) {
                requests.refresh()
                snackbarHostState?.showSnackbar(
                    message = getString(Res.string.network_request_accepted)
                )
            }
        }
    }
    navController?.collectResult(
        key = NavigationArguments.NETWORK_NEW_SUCCESS,
        defaultValue = false,
        listener = { isSuccess ->
            if(isSuccess) requests.refresh()
        }
    )

    RefreshableContent(
        onRefresh = {
            viewModel.requestData(isSpecial = true, isPullRefresh = true)
            requests.refresh()
        },
        isRefreshing = isRefreshing.value
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(
                count = requests.itemCount,
                key = { index -> requests[index]?.uid ?: Uuid.random().toString() }
            ) { index ->
                requests[index].let { data ->
                    CircleSentRequestRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        data = data,
                        response = response.value[data?.uid],
                        onResponse = { accept ->
                            if(data?.uid != null) viewModel.acceptRequest(uid = data.uid, accept = accept)
                        }
                    )
                    if(requests.itemCount - 1 != index) {
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth(),
                            color = LocalTheme.current.colors.tetrial,
                            thickness = .3.dp
                        )
                    }
                }
            }
        }
    }
}
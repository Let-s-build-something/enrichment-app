package ui.dev.experiments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import augmy.interactive.shared.ext.scalingClickable
import augmy.interactive.shared.ui.components.ComponentHeaderButton
import augmy.interactive.shared.ui.components.ProgressPressableContainer
import augmy.interactive.shared.ui.components.dialog.AlertDialog
import augmy.interactive.shared.ui.components.dialog.ButtonState
import augmy.interactive.shared.ui.theme.LocalTheme
import augmy.interactive.shared.ui.theme.SharedColors
import augmy.interactive.shared.utils.DateUtils
import data.io.experiment.ExperimentIO
import data.io.experiment.ExperimentSet
import data.io.experiment.FullExperiment
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ExperimentsContent(model: ExperimentModel = koinViewModel()) {
    val experiments = model.experiments.collectAsState()

    val selectedExperiment = remember(model) {
        mutableStateOf<FullExperiment?>(null)
    }

    selectedExperiment.value?.let { experiment ->
        EditExperimentDialog(
            experiment = experiment,
            model = model,
            onDismissRequest = { selectedExperiment.value = null }
        )
    }

    LazyColumn {
        item(key = "title") {
            Text(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .background(color = LocalTheme.current.colors.appbarBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                text = "All experiments",
                style = LocalTheme.current.styles.subheading.copy(
                    color = LocalTheme.current.colors.appbarContent
                )
            )
        }
        stickyHeader(key = "addNewButton") {
            ComponentHeaderButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .fillMaxWidth(),
                text = "New experiment",
                endImageVector = Icons.Outlined.Add
            ) {
                model.createExperiment(ExperimentIO(name = "Experiment ${experiments.value.size}"))
            }
        }
        items(
            items = experiments.value,
            key = { it.data.uid }
        ) { experiment ->
            Column(modifier = Modifier.animateItem()) {
                Row(
                    modifier = Modifier
                        .scalingClickable(key = experiment.data.uid, scaleInto = .95f) {
                            selectedExperiment.value = experiment
                        }
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = experiment.data.fullName,
                            style = LocalTheme.current.styles.category
                        )
                        Text(
                            text = experiment.sets.joinToString(", ") { it.name },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = LocalTheme.current.styles.regular
                        )
                    }
                    experiment.data.activateUntil?.let { until ->
                        val millisUntil = DateUtils.now.toEpochMilliseconds().minus(until)
                        // TODO countdown
                        Text(
                            text = if (millisUntil > 0) DateUtils.formatMillis(millisUntil) else "Inactive",
                            style = LocalTheme.current.styles.regular.copy(
                                color = if (millisUntil <= 0) {
                                    LocalTheme.current.colors.disabledComponent
                                } else LocalTheme.current.colors.disabled
                            )
                        )
                    }
                }
            }
        }
        item(key = "bottomPadding") {
            Spacer(Modifier.height(50.dp))
        }
    }
}

@Composable
private fun EditExperimentDialog(
    experiment: FullExperiment,
    model: ExperimentModel,
    onDismissRequest: () -> Unit
) {
    val showDeleteConfirmation = remember(experiment.data.uid) {
        mutableStateOf(false)
    }
    val showSetSelection = remember(experiment.data.uid) {
        mutableStateOf(false)
    }
    val selectedSet = remember(experiment.data.uid) {
        mutableStateOf(experiment.sets.firstOrNull())
    }

    if (showDeleteConfirmation.value) {
        AlertDialog(
            title = "Delete experiment",
            message = AnnotatedString("Are you sure you want to delete this experiment?"),
            confirmButtonState = ButtonState(
                text = "Confirm",
                onClick = {
                    model.deleteExperiment(experiment.data.uid)
                    showDeleteConfirmation.value = false
                }
            ),
            onDismissRequest = { showDeleteConfirmation.value = false }
        )
    } else if (showSetSelection.value) {
        val setPick = remember(experiment.data.uid) {
            mutableStateOf<ExperimentSet?>(null)
        }

        AlertDialog(
            title = "Select set of values",
            intrinsicContent = false,
            additionalContent = {
                val sets = model.sets.collectAsState()

                LazyColumn(modifier = Modifier.fillMaxHeight(.5f)) {
                    stickyHeader(key = "addNewButton") {
                        ComponentHeaderButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .fillMaxWidth(),
                            text = "New set",
                            endImageVector = Icons.Outlined.Add
                        ) {
                            model.createExperiment(ExperimentIO(name = "Set ${sets.value.size}"))
                        }
                    }
                    items(items = sets.value, key = { it.uid }) { set ->
                        Column(
                            modifier = Modifier
                                .scalingClickable(key = set.uid) {
                                    setPick.value = set
                                }
                                .animateItem()
                                .then(
                                    if (setPick.value?.uid == set.uid){
                                        Modifier
                                            .background(
                                                color = LocalTheme.current.colors.appbarBackground,
                                                shape = LocalTheme.current.shapes.rectangularActionShape
                                            )
                                            .border(
                                                width = .5.dp,
                                                color = LocalTheme.current.colors.disabled,
                                                shape = LocalTheme.current.shapes.rectangularActionShape
                                            )
                                    } else Modifier
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "${set.name}, (${set.values.size} values)",
                                style = LocalTheme.current.styles.category
                            )
                            Text(
                                text = set.values.joinToString(","),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = LocalTheme.current.styles.regular
                            )
                        }
                    }
                }
            },
            confirmButtonState = ButtonState(
                enabled = setPick.value != null,
                text = "Confirm",
                onClick = {
                    setPick.value?.uid?.let {
                        model.changeSetOf(experiment.data.uid, it)
                        selectedSet.value = setPick.value
                    }
                }
            ),
            onDismissRequest = { showDeleteConfirmation.value = false }
        )
    }

    AlertDialog(
        title = experiment.data.name ?: experiment.data.uid,
        additionalContent = {
            // TODO ability to change sets, name, and frequency
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Set: ",
                    style = LocalTheme.current.styles.regular
                )
                Text(
                    modifier = Modifier
                        .scalingClickable {
                            showSetSelection.value = true
                        },
                    text = experiment.sets.firstOrNull()?.name ?: "Select",
                    style = LocalTheme.current.styles.regular
                )
            }

            Row {
                ProgressPressableContainer(
                    modifier = Modifier.requiredSize(36.dp),
                    onFinish = {
                        showDeleteConfirmation.value = true
                    },
                    trackColor = LocalTheme.current.colors.disabled,
                    progressColor = SharedColors.RED_ERROR
                ) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = SharedColors.RED_ERROR
                    )
                }
            }
        },
        onDismissRequest = onDismissRequest
    )
}

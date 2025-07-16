package data.io.experiment

data class FullExperiment(
    val data: ExperimentIO,
    val sets: List<ExperimentSet> = listOf()
)

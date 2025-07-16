package ui.dev.experiments

import androidx.lifecycle.viewModelScope
import data.io.experiment.ExperimentIO
import data.io.experiment.ExperimentSet
import data.io.experiment.FullExperiment
import data.shared.SharedModel
import database.dao.experiment.ExperimentDao
import database.dao.experiment.ExperimentSetDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val experimentModule = module {
    factory { ExperimentRepository(get(), get()) }
    viewModelOf(::ExperimentModel)
}

class ExperimentModel(
    private val repository: ExperimentRepository
): SharedModel() {
    private val _experiments = MutableStateFlow(listOf<FullExperiment>())
    private val _sets = MutableStateFlow(listOf<ExperimentSet>())

    val experiments = _experiments.asStateFlow()
    val sets = _sets.asStateFlow()

    init {
        getSets()
        getExperiments()
    }

    private fun getSets() {
        viewModelScope.launch {
            _sets.value = repository.getExperimentSets()
        }
    }

    private fun getExperiments() {
        viewModelScope.launch {
            _experiments.value = repository.getExperiments(matrixUserId).map { experiment ->
                FullExperiment(
                    data = experiment,
                    sets = _sets.value.filter { it.uid in experiment.setUids }
                )
            }
        }
    }

    fun createExperiment(experiment: ExperimentIO) {
        viewModelScope.launch {
            repository.insertExperiment(experiment)
            _experiments.update {
                it.plus(FullExperiment(experiment))
            }
        }
    }

    fun changeSetOf(experimentUid: String, setUid: String) {
        viewModelScope.launch {
            repository.insertExperiment(
                _experiments.value.first { it.data.uid == experimentUid }.data.copy(
                    setUids = listOf(setUid)
                )
            )
        }
    }

    fun deleteExperiment(uid: String) {
        viewModelScope.launch {
            repository.deleteExperiment(uid)
            _experiments.update {
                it.filter { experiment ->
                    experiment.data.uid != uid
                }
            }
        }
    }

    // TODO activation
}

class ExperimentRepository(
    private val experimentDao: ExperimentDao,
    private val experimentSetDao: ExperimentSetDao
) {
    suspend fun getExperiments(owner: String?) = experimentDao.getAll(owner)
    suspend fun insertExperiment(experiment: ExperimentIO) = experimentDao.insert(experiment)
    suspend fun getExperimentSets() = experimentSetDao.getAll()
    suspend fun deleteExperiment(uid: String) = experimentDao.remove(uid)
}

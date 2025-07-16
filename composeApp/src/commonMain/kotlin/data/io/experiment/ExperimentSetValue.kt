package data.io.experiment

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class ExperimentSetValue @OptIn(ExperimentalUuidApi::class) constructor(
    val value: String,
    val uid: String = Uuid.random().toString()
)
package data.io.matrix.auth

import kotlinx.serialization.Serializable

@Serializable
data class MatrixVersions(
    val versions: List<String>? = null
)
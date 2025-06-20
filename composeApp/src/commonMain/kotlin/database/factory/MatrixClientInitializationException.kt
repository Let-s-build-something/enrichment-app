package database.factory

import kotlinx.serialization.Serializable

@Serializable
sealed interface MatrixClientInitializationException {
    @Serializable
    data class DatabaseAccessException(override val message: String? = null) : MatrixClientInitializationException,
        RuntimeException(message)

    @Serializable
    data class DatabaseLockedException(override val message: String? = null) : MatrixClientInitializationException,
        RuntimeException(message)

    @Serializable
    data object NoDatabaseException : MatrixClientInitializationException, RuntimeException("no database existing")

    @Serializable
    data class Unknown(override val message: String? = null) : MatrixClientInitializationException,
        RuntimeException(message)
}
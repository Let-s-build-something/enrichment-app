package data.sensor

import augmy.interactive.shared.utils.DateUtils

data class SensorEvent(
    //val sensor: Sensor,
    val values: FloatArray?,
    val timestamp: Long = DateUtils.now.toEpochMilliseconds(),
    val accuracy: Int? = null,
    val visibleWindowValues: List<VisibleWindowValue>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SensorEvent

        if (accuracy != other.accuracy) return false
        if (timestamp != other.timestamp) return false
        if (values != null) {
            if (other.values == null) return false
            if (!values.contentEquals(other.values)) return false
        } else if (other.values != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accuracy
        result = 31 * (result ?: 0) + timestamp.hashCode()
        result = 31 * result + (values?.contentHashCode() ?: 0)
        return result
    }
}

data class VisibleWindowValue(
    val name: String?,
    val command: String?
)

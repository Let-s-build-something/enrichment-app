package data.sensor

data class SensorEvent(
    //val sensor: Sensor,
    val accuracy: Int?,
    val timestamp: Long?,
    val values: FloatArray?
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
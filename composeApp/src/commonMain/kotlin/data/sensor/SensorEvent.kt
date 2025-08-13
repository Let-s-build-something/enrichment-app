package data.sensor

import augmy.interactive.shared.utils.DateUtils

data class SensorEvent(
    val timestamp: String = DateUtils.localNow.toString(),
    val values: FloatArray? = null,
    val uiValues: Map<String?, String?>? = null
) {

    override fun hashCode(): Int {
        return this::class.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SensorEvent

        if (timestamp != other.timestamp) return false
        if (values != null) {
            if (other.values == null) return false
            if (!values.contentEquals(other.values)) return false
        } else if (other.values != null) return false
        if (uiValues != other.uiValues) return false

        return true
    }
}

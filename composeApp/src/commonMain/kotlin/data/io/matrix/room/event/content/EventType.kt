package data.io.matrix.room.event.content

import kotlin.reflect.KClass

data class EventType(val kClass: KClass<out EventContent>?, val name: String) {
    override fun toString(): String = name
}
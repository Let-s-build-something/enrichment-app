package data.io.matrix.room.event.content

interface Event<C : EventContent> {
    val content: C
}
package data.io.matrix.room.event.content.key


interface VerificationRequest {
    val fromDevice: String
    val methods: Set<VerificationMethod>
}
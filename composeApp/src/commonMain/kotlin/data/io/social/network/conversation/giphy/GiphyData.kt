package data.io.social.network.conversation.giphy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Concrete information about a Giphy */
@Serializable
data class GiphyData(
    val id: String? = null,

    /** Directory of images contained in this data */
    val images: GiphyImages? = null,

    /** Author username */
    val username: String? = null,

    /** Content age rating */
    val rating: String? = null,

    /** Title defined by the author */
    val title: String? = null,

    /** Textual description of the gif */
    @SerialName("alt_text")
    val altText: String? = null,

    /** Url to original Giphy detail */
    val url: String? = null
) {
    /** Unique identifier assigned to this data object, since GIPHY sends duplicate data */
    @OptIn(ExperimentalUuidApi::class)
    @Transient
    val uid: String = Uuid.random().toString()
}

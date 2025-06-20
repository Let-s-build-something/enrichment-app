package ui.conversation.components.link

/** Open Graph Protocol data from a website */
data class GraphProtocol(
    /** website favicon */
    val iconUrl: String? = null,

    /** Title of the site */
    val title: String? = null,

    /** Description of the site */
    val description: String? = null,

    /** Url of an image preview */
    val imageUrl: String? = null
) {
    /** Whether there is no content for the preview */
    val isEmpty: Boolean
        get() = title?.isBlank() != false
                && description?.isBlank() != false
                && imageUrl?.isBlank() != false
                && iconUrl?.isBlank() != false

    override fun toString(): String {
        return "{" +
                "iconUrl=$iconUrl, " +
                "title=$title, " +
                "description=$description, " +
                "imageUrl=$imageUrl" +
                "}"
    }
}
package data.io.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Information about current pagination */
@Serializable
data class PaginationInfo(
    /** number of current page, starts from 0 */
    val page: Int,

    /** number of elements at a single page */
    val size: Int,

    /** total number of pages */
    @SerialName("total_pages")
    val totalPages: Int = 0,

    /** total number of items */
    @SerialName("total_items")
    val totalItems: Int = totalPages * size
)
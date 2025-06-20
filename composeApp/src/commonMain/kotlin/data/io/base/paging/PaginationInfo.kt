package data.io.base.paging

import kotlinx.serialization.Serializable

/** Information about current pagination */
@Serializable
data class PaginationInfo(
    /** number of current page, starts from 0 */
    val page: Int,

    /** number of elements at a single page */
    val size: Int,

    /** total number of pages */
    val totalPages: Int = 0,

    /** total number of items */
    val totalItems: Int = totalPages * size
)
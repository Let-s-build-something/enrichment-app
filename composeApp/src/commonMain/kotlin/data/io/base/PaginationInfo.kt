package data.io.base

/** Information about current pagination */
data class PaginationInfo(
    /** number of current page, starts from 0 */
    val page: Int,

    /** number of elements at a single page */
    val size: Int,

    /** total number of pages */
    val totalPages: Int
)
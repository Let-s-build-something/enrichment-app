package data.io.base.paging

/** standard shell for pagination response */
interface PaginationPageResponse<T> {
    /** information about current pagination response */
    val pagination: PaginationInfo?

    /** list of content for the current page */
    val content: List<T>
}
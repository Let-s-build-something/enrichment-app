package data.io.social.network

import data.io.base.PaginationInfo
import data.io.base.PaginationPageResponse

/** Response to the request for circling requests */
data class CirclingRequestsResponse(
	override val pagination: PaginationInfo,
	override val content: List<CirclingRequest>
): PaginationPageResponse<CirclingRequest>
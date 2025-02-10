package ui.home

import androidx.paging.Pager
import androidx.paging.PagingConfig
import data.io.base.BaseResponse
import data.io.base.paging.PaginationInfo
import data.io.matrix.room.ConversationRoomIO
import data.io.social.network.conversation.ConversationListResponse
import database.dao.ConversationRoomDao

class HomeRepository(private val conversationRoomDao: ConversationRoomDao) {

    /** Returns a flow of network list */
    fun getConversationRoomPager(
        config: PagingConfig,
        ownerPublic: () -> String?
    ): Pager<Int, ConversationRoomIO> {
        return Pager(
            config = config,
            pagingSourceFactory = {
                ConversationRoomSource(
                    size = config.pageSize,
                    getItems = { page ->
                        val res = conversationRoomDao.getPaginated(
                            ownerPublicId = ownerPublic(),
                            limit = config.pageSize,
                            offset = page * config.pageSize
                        )

                        BaseResponse.Success(
                            ConversationListResponse(
                                content = res,
                                pagination = PaginationInfo(
                                    page = page,
                                    size = res.size,
                                    totalItems = conversationRoomDao.getCount(ownerPublic())
                                )
                            )
                        )
                    }
                )
            }
        )
    }
}

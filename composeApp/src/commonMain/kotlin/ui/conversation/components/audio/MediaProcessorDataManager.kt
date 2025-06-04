package ui.conversation.components.audio

import data.io.base.BaseResponse
import data.io.social.network.conversation.message.MediaIO
import kotlinx.coroutines.flow.MutableStateFlow

class MediaProcessorDataManager {

    val cachedFiles = MutableStateFlow<Map<String, BaseResponse<MediaIO>>>(mapOf())
}
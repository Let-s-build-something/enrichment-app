package base

import androidx.compose.ui.graphics.Color
import app.cash.paging.compose.LazyPagingItems

/** Returns item at a specific index and handles indexOutOfBounds exception */
fun <T: Any> LazyPagingItems<T>.getOrNull(index: Int): T? {
    return if(index >= this.itemCount) null else this[index]
}

/** Color derived from a user tag */
fun tagToColor(tag: String?) = if(tag != null) Color(("ff$tag").toLong(16)) else null
package base

import app.cash.paging.compose.LazyPagingItems

/** Returns item at a specific index and handles indexOutOfBounds exception */
fun <T: Any> LazyPagingItems<T>.getOrNull(index: Int): T? {
    return if(index >= this.itemCount) null else this[index]
}
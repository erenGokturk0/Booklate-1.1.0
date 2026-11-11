package com.erengokturk.booklate.ui.navigation

object Routes {
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val READER = "reader/{bookId}"
    const val ARG_BOOK_ID = "bookId"

    fun reader(bookId: String): String = "reader/$bookId"
}

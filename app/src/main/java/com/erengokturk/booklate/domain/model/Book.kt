package com.erengokturk.booklate.domain.model

/**
 * Kütüphanedeki bir PDF kitabını temsil eder.
 *
 * Dosya, içe aktarma sırasında uygulamanın özel dizinine kopyalanır. Böylece
 * SAF (Storage Access Framework) izinleri kalıcı olmasa bile kitap açılmaya devam eder.
 */
data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val filePath: String,
    val sourceUri: String?,
    val fileName: String,
    val pageCount: Int,
    val lastPage: Int,
    val lastPageOffset: Float,
    val coverPath: String?,
    val fileSize: Long,
    val createdAt: Long,
    val lastOpenedAt: Long,
    /** Kitabın yazıldığı/okunduğu dil. Her kitap için ayrı seçilir. */
    val languageCode: String = "ru",
) {
    val progress: Float
        get() = if (pageCount <= 0) 0f else ((lastPage + 1f) / pageCount).coerceIn(0f, 1f)

    val displayPage: Int
        get() = if (pageCount <= 0) 0 else (lastPage + 1).coerceIn(1, pageCount)

    val safeLastPage: Int
        get() = if (pageCount <= 0) 0 else lastPage.coerceIn(0, pageCount - 1)
}

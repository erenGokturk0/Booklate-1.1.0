package com.erengokturk.booklate.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.erengokturk.booklate.domain.model.Book

@Entity(
    tableName = "books",
    indices = [Index("lastOpenedAt"), Index("sourceUri")],
)
data class BookEntity(
    @PrimaryKey val id: String,
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
    @ColumnInfo(defaultValue = "ru") val languageCode: String = "ru",
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        author = author,
        filePath = filePath,
        sourceUri = sourceUri,
        fileName = fileName,
        pageCount = pageCount,
        lastPage = lastPage,
        lastPageOffset = lastPageOffset,
        coverPath = coverPath,
        fileSize = fileSize,
        createdAt = createdAt,
        lastOpenedAt = lastOpenedAt,
        languageCode = languageCode,
    )

    companion object {
        fun fromDomain(book: Book): BookEntity = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            filePath = book.filePath,
            sourceUri = book.sourceUri,
            fileName = book.fileName,
            pageCount = book.pageCount,
            lastPage = book.lastPage,
            lastPageOffset = book.lastPageOffset,
            coverPath = book.coverPath,
            fileSize = book.fileSize,
            createdAt = book.createdAt,
            lastOpenedAt = book.lastOpenedAt,
            languageCode = book.languageCode,
        )
    }
}

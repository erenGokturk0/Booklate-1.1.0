package com.erengokturk.booklate.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.erengokturk.booklate.data.local.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastOpenedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE sourceUri = :sourceUri LIMIT 1")
    suspend fun findBySourceUri(sourceUri: String): BookEntity?

    @Upsert
    suspend fun upsert(book: BookEntity)

    @Query(
        """
        UPDATE books
        SET lastPage = :page, lastPageOffset = :offset, lastOpenedAt = :openedAt
        WHERE id = :id
        """,
    )
    suspend fun updateProgress(id: String, page: Int, offset: Float, openedAt: Long)

    @Query("UPDATE books SET lastOpenedAt = :openedAt WHERE id = :id")
    suspend fun touchOpened(id: String, openedAt: Long)

    @Query("UPDATE books SET languageCode = :code WHERE id = :id")
    suspend fun updateLanguage(id: String, code: String)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: String)
}

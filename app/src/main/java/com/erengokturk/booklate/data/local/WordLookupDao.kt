package com.erengokturk.booklate.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WordLookupDao {
    @Query("SELECT * FROM word_lookups WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun find(cacheKey: String): WordLookupEntity?

    @Query("SELECT * FROM word_lookups ORDER BY lastAccessedAt DESC")
    fun observeAll(): Flow<List<WordLookupEntity>>

    @Query(
        """
        SELECT * FROM word_lookups
        WHERE :query = '' OR word LIKE '%' || :query || '%'
           OR sentence LIKE '%' || :query || '%'
           OR translation LIKE '%' || :query || '%'
        ORDER BY lastAccessedAt DESC
        """,
    )
    fun search(query: String): Flow<List<WordLookupEntity>>

    @Query("SELECT * FROM word_lookups WHERE isFavorite = 1 ORDER BY lastAccessedAt DESC")
    fun observeFavorites(): Flow<List<WordLookupEntity>>

    @Upsert
    suspend fun upsert(entry: WordLookupEntity)

    @Query(
        """
        UPDATE word_lookups
        SET hitCount = hitCount + 1, lastAccessedAt = :now
        WHERE cacheKey = :cacheKey
        """,
    )
    suspend fun touch(cacheKey: String, now: Long)

    @Query("UPDATE word_lookups SET isFavorite = :favorite WHERE cacheKey = :cacheKey")
    suspend fun setFavorite(cacheKey: String, favorite: Boolean)

    @Query("DELETE FROM word_lookups WHERE cacheKey = :cacheKey")
    suspend fun delete(cacheKey: String)

    @Query("DELETE FROM word_lookups")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM word_lookups")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM word_lookups WHERE isFavorite = 1")
    suspend fun favoriteCount(): Int
}

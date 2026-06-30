package com.taleson2wheels.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One cached `/api/v1` response, keyed by a stable logical name (e.g.
 * `"rides:first"`, `"ride:<id>"`). `json` is the serialized DTO and `updatedAt`
 * is the wall-clock millis when it was written — used for age-based eviction.
 */
@Entity(tableName = "cached_responses")
data class CachedResponse(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAt: Long,
)

@Dao
interface CachedResponseDao {

    @Query("SELECT * FROM cached_responses WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CachedResponse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CachedResponse)

    @Query("DELETE FROM cached_responses WHERE updatedAt < :cutoff")
    suspend fun evictOlderThan(cutoff: Long)
}

package com.axiomlabs.vidamacraft.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAtEpochMillis DESC, id DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(download: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null): Int

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes WHERE id = :id")
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long): Int

    @Query("UPDATE downloads SET localUri = :localUri WHERE id = :id")
    suspend fun updateLocalUri(id: Long, localUri: String): Int

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM downloads WHERE status IN (:statuses)")
    suspend fun deleteByStatuses(statuses: List<DownloadStatus>): Int
}

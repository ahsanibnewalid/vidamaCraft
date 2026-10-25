package com.axiomlabs.vidamacraft.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One row per download job. Byte counts are persisted so progress can be restored. */
@Entity(
    tableName = "downloads",
    indices = [Index(value = ["createdAtEpochMillis"]), Index(value = ["status"])]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val fileName: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /** Android DownloadManager ID, or -1 when no OS job has been associated yet. */
    val systemDownloadId: Long = -1L,
    val downloadedBytes: Long = 0L,
    /** -1 means the server did not provide a known content length. */
    val totalBytes: Long = -1L,
    val localUri: String? = null,
    val errorMessage: String? = null
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

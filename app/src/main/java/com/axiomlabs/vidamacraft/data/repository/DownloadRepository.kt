package com.axiomlabs.vidamacraft.data.repository

import androidx.room.withTransaction
import com.axiomlabs.vidamacraft.data.local.DownloadDao
import com.axiomlabs.vidamacraft.data.local.DownloadEntity
import com.axiomlabs.vidamacraft.data.local.DownloadStatus
import com.axiomlabs.vidamacraft.data.local.VidamaDatabase
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun observeDownloads(): Flow<List<DownloadEntity>>
    suspend fun getDownload(id: Long): DownloadEntity?
    suspend fun createDownload(url: String, fileName: String): Long
    suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String? = null)
    suspend fun updateSystemDownloadId(id: Long, systemDownloadId: Long)
    suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long)
    suspend fun markCompleted(id: Long, downloadedBytes: Long, totalBytes: Long, localUri: String)
    suspend fun deleteDownload(id: Long)
}

/** Persistence layer for download records and their Android DownloadManager association. */
class RoomDownloadRepository(
    private val database: VidamaDatabase,
    private val dao: DownloadDao = database.downloadDao()
) : DownloadRepository {
    override fun observeDownloads(): Flow<List<DownloadEntity>> = dao.observeAll()

    override suspend fun getDownload(id: Long): DownloadEntity? = dao.getById(id)

    override suspend fun createDownload(url: String, fileName: String): Long {
        require(url.startsWith("https://", ignoreCase = true)) { "Only HTTPS URLs are supported." }
        require(url.length <= 8_192) { "URL is too long." }
        val safeName = fileName.trim().replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .trim('.')
            .take(120)
            .ifBlank { "download.bin" }
        return dao.insert(DownloadEntity(url = url.trim(), fileName = safeName))
    }

    override suspend fun updateStatus(id: Long, status: DownloadStatus, errorMessage: String?) {
        dao.updateStatus(id, status, errorMessage?.take(500))
    }

    override suspend fun updateSystemDownloadId(id: Long, systemDownloadId: Long) {
        require(systemDownloadId >= 0L) { "Android download ID must be non-negative." }
        check(dao.updateSystemDownloadId(id, systemDownloadId) == 1) {
            "Download record $id no longer exists."
        }
    }

    override suspend fun updateProgress(id: Long, downloadedBytes: Long, totalBytes: Long) {
        require(downloadedBytes >= 0L) { "Downloaded bytes cannot be negative." }
        require(totalBytes >= -1L) { "Total bytes must be -1 (unknown) or non-negative." }
        dao.updateProgress(id, downloadedBytes, totalBytes)
    }

    override suspend fun markCompleted(id: Long, downloadedBytes: Long, totalBytes: Long, localUri: String) {
        require(downloadedBytes >= 0L && totalBytes >= -1L)
        database.withTransaction {
            dao.updateProgress(id, downloadedBytes, totalBytes)
            dao.updateLocalUri(id, localUri)
            dao.updateStatus(id, DownloadStatus.COMPLETED)
        }
    }

    override suspend fun deleteDownload(id: Long) {
        dao.deleteById(id)
    }
}

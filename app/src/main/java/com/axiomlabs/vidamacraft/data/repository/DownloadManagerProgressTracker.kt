package com.axiomlabs.vidamacraft.data.repository

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.axiomlabs.vidamacraft.data.local.DownloadEntity
import com.axiomlabs.vidamacraft.data.local.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads Android's authoritative transfer state and mirrors it into Room. */
@Singleton
class DownloadManagerProgressTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: DownloadRepository
) {
    private val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    suspend fun refresh(download: DownloadEntity) = withContext(Dispatchers.IO) {
        if (download.systemDownloadId < 0L || download.status in TERMINAL_STATUSES) return@withContext

        val query = DownloadManager.Query().setFilterById(download.systemDownloadId)
        val cursor: Cursor = try {
            manager.query(query) ?: run {
                repository.updateStatus(download.id, DownloadStatus.FAILED, "Android download record is no longer available.")
                return@withContext
            }
        } catch (_: SecurityException) {
            repository.updateStatus(download.id, DownloadStatus.FAILED, "Unable to read Android download status.")
            return@withContext
        }

        cursor.use {
            if (!it.moveToFirst()) {
                repository.updateStatus(download.id, DownloadStatus.FAILED, "Android download record is no longer available.")
                return@withContext
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)).coerceAtLeast(0L)
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).let { size ->
                if (size < 0L) -1L else size
            }
            when (status) {
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> {
                    repository.updateProgress(download.id, bytes, total)
                    repository.updateStatus(download.id, DownloadStatus.DOWNLOADING)
                }
                DownloadManager.STATUS_PAUSED -> {
                    repository.updateProgress(download.id, bytes, total)
                    repository.updateStatus(download.id, DownloadStatus.PAUSED)
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        ?: Uri.parse("content://downloads/my_downloads/${download.systemDownloadId}").toString()
                    repository.markCompleted(download.id, bytes, total, localUri)
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    repository.updateProgress(download.id, bytes, total)
                    repository.updateStatus(download.id, DownloadStatus.FAILED, "Android DownloadManager error code: $reason")
                }
            }
        }
    }

    private companion object {
        val TERMINAL_STATUSES = setOf(DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED)
    }
}

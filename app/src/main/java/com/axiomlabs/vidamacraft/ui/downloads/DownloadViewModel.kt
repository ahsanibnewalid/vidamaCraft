package com.axiomlabs.vidamacraft.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.axiomlabs.vidamacraft.data.local.DownloadStatus
import com.axiomlabs.vidamacraft.data.repository.DownloadManagerProgressTracker
import com.axiomlabs.vidamacraft.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository,
    private val progressTracker: DownloadManagerProgressTracker
) : ViewModel() {
    private val submitting = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DownloadUiState> = combine(
        repository.observeDownloads(), submitting, message
    ) { downloads, isSubmitting, currentMessage ->
        DownloadUiState(downloads = downloads, isSubmitting = isSubmitting, message = currentMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUiState())

    init {
        // DownloadManager continues independently; this loop reconciles its state when the app is visible/alive.
        viewModelScope.launch {
            while (isActive) {
                try {
                    repository.observeDownloads().first()
                        .filter { it.systemDownloadId >= 0L && it.status !in setOf(
                            DownloadStatus.COMPLETED, DownloadStatus.FAILED, DownloadStatus.CANCELLED
                        ) }
                        .forEach { progressTracker.refresh(it) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A transient query failure must not crash the screen; retry on the next cycle.
                }
                delay(1_000)
            }
        }
    }

    fun queueDownload(url: String, fileName: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            submitting.value = true
            message.value = null
            try {
                val id = repository.createDownload(url, fileName)
                message.value = "Download record #$id saved."
                onCreated(id)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IllegalArgumentException) {
                message.value = error.message ?: "Please check the URL and filename."
            } catch (_: Exception) {
                message.value = "Could not save or start this download. Please try again."
            } finally {
                submitting.value = false
            }
        }
    }

    fun setSystemDownloadId(recordId: Long, systemDownloadId: Long) {
        viewModelScope.launch {
            try {
                repository.updateSystemDownloadId(recordId, systemDownloadId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = "The download started, but its tracking ID could not be saved."
            }
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteDownload(id)
                message.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = "Could not delete this history item."
            }
        }
    }

    fun setStatus(id: Long, status: DownloadStatus, errorMessage: String? = null) {
        viewModelScope.launch {
            try {
                repository.updateStatus(id, status, errorMessage)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message.value = "Could not update the download status."
            }
        }
    }

    fun clearMessage() { message.value = null }
}

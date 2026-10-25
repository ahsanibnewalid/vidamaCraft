package com.axiomlabs.vidamacraft.ui.downloads

import com.axiomlabs.vidamacraft.data.local.DownloadEntity

 data class DownloadUiState(
    val downloads: List<DownloadEntity> = emptyList(),
    val isSubmitting: Boolean = false,
    val message: String? = null
)

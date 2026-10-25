package com.axiomlabs.vidamacraft.data.local

import androidx.room.TypeConverter

class DownloadConverters {
    @TypeConverter
    fun statusToString(status: DownloadStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.FAILED)
}

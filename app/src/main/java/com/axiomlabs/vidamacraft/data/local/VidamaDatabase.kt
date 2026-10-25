package com.axiomlabs.vidamacraft.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [DownloadEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DownloadConverters::class)
abstract class VidamaDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}

package com.axiomlabs.vidamacraft.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DownloadEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(DownloadConverters::class)
abstract class VidamaDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        /** Adds the OS download identifier without discarding existing download history. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE downloads ADD COLUMN systemDownloadId INTEGER NOT NULL DEFAULT -1"
                )
            }
        }
    }
}

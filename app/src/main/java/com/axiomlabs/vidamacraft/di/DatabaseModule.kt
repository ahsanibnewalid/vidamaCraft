package com.axiomlabs.vidamacraft.di

import android.content.Context
import androidx.room.Room
import com.axiomlabs.vidamacraft.data.local.DownloadDao
import com.axiomlabs.vidamacraft.data.local.VidamaDatabase
import com.axiomlabs.vidamacraft.data.repository.DownloadRepository
import com.axiomlabs.vidamacraft.data.repository.RoomDownloadRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideVidamaDatabase(@ApplicationContext context: Context): VidamaDatabase =
        Room.databaseBuilder(context, VidamaDatabase::class.java, "vidamacraft_downloads.db")
            .addMigrations(VidamaDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideDownloadDao(database: VidamaDatabase): DownloadDao = database.downloadDao()

    @Provides
    @Singleton
    fun provideDownloadRepository(database: VidamaDatabase): DownloadRepository =
        RoomDownloadRepository(database)
}

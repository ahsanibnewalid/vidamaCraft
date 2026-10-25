package com.axiomlabs.vidamacraft.sandbox

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException

class SandboxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            Log.i(TAG, "Extractor wrapper initialized")
        } catch (error: YoutubeDLException) {
            Log.e(TAG, "Extractor wrapper initialization failed", error)
        }
    }

    companion object {
        private const val TAG = "VidamaExtractor"
    }
}

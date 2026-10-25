package com.axiomlabs.vidamacraft.sandbox

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var output: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        output = TextView(this).apply {
            text = "Metadata-only extractor sandbox\\n\\nTap Test metadata to query an authorized test URL."
            textSize = 16f
            setPadding(32, 24, 32, 24)
        }

        val testButton = Button(this).apply {
            text = "Test metadata"
            setOnClickListener { queryMetadata() }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(testButton)
            addView(ScrollView(this@MainActivity).apply { addView(output) })
        }
        setContentView(layout)
    }

    private fun queryMetadata() {
        val testUrl = "https://vimeo.com/22439234"
        output.text = "Querying metadata…"
        executor.execute {
            val result = runCatching {
                val request = YoutubeDLRequest(testUrl)
                YoutubeDL.getInstance().getInfo(request)
            }
            runOnUiThread {
                result.onSuccess { info ->
                    val formats = info.formats.orEmpty()
                    val summary = buildString {
                        appendLine("Title: ${info.title ?: "Unavailable"}")
                        appendLine("Duration: ${info.duration ?: "Unavailable"} seconds")
                        appendLine("Formats found: ${formats.size}")
                        formats.take(30).forEach { format ->
                            appendLine("• ${format.formatId ?: "?"} | ${format.ext ?: "?"} | ${format.resolution ?: "unknown resolution"}")
                        }
                    }
                    output.text = summary
                    Log.i("VidamaExtractor", summary)
                }.onFailure { error ->
                    output.text = "Metadata query failed: ${error.message ?: error.javaClass.simpleName}"
                    Log.e("VidamaExtractor", "getInfo failed", error)
                }
            }
        }
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}

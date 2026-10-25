package com.axiomlabs.vidamacraft.sandbox

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.Locale
import java.util.concurrent.Executors

private data class FormatOption(
    val id: String,
    val extension: String,
    val resolution: String?,
    val estimatedSize: String?,
    val category: FormatCategory,
    val rawFormat: Any
) {
    val displayName: String
        get() = buildList {
            add(resolution ?: if (category == FormatCategory.AUDIO_ONLY) "Audio" else "Format $id")
            if (extension.isNotBlank()) add(extension.uppercase(Locale.ROOT))
            estimatedSize?.let { add("~$it") }
        }.joinToString(" • ")
}

private enum class FormatCategory(val label: String) {
    VIDEO_AUDIO("Video with audio"),
    VIDEO_ONLY("Video only"),
    AUDIO_ONLY("Audio only"),
    OTHER("Other formats")
}

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    private var loading by mutableStateOf(false)
    private var title by mutableStateOf("Metadata-only extractor sandbox")
    private var duration by mutableStateOf<String?>(null)
    private var status by mutableStateOf("Tap Test metadata to query the authorized sample URL.")
    private var formats by mutableStateOf<List<FormatOption>>(emptyList())
    private var showPicker by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SandboxScreen(
                        title = title,
                        duration = duration,
                        status = status,
                        loading = loading,
                        formats = formats,
                        showPicker = showPicker,
                        onTestMetadata = ::queryMetadata,
                        onOpenPicker = { showPicker = true },
                        onDismissPicker = { showPicker = false },
                        onFormatSelected = { option ->
                            showPicker = false
                            Toast.makeText(
                                this,
                                "Selected ${option.displayName}. Download is not wired up yet.",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.i("VidamaExtractor", "Selected format id=${option.id}, category=${option.category}")
                        }
                    )
                }
            }
        }
    }

    private fun queryMetadata() {
        if (loading) return
        val testUrl = "https://vimeo.com/22439234"
        loading = true
        status = "Querying metadata…"
        executor.execute {
            val result = runCatching {
                val request = YoutubeDLRequest(testUrl)
                YoutubeDL.getInstance().getInfo(request)
            }
            runOnUiThread {
                loading = false
                result.onSuccess { info ->
                    title = info.title ?: "Untitled video"
                    duration = info.duration?.let { "${it} seconds" }
                    formats = info.formats.orEmpty().mapNotNull { format ->
                        val id = format.formatId?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        val ext = format.ext?.toString().orEmpty()
                        val resolution = readString(format, "height", "resolution", "formatNote", "format_note")
                            ?.let { raw ->
                                val height = raw.toIntOrNull()
                                if (height != null && height > 0) "${height}p"
                                else Regex("(?i)([0-9]{3,4}p)").find(raw)?.value
                                    ?: raw.takeIf { it.contains("audio", ignoreCase = true) }
                            }
                        val vcodec = readString(format, "vcodec", "videoCodec", "video_codec")
                        val acodec = readString(format, "acodec", "audioCodec", "audio_codec")
                        val category = when {
                            vcodec.isVideoCodec() && acodec.isAudioCodec() -> FormatCategory.VIDEO_AUDIO
                            vcodec.isVideoCodec() -> FormatCategory.VIDEO_ONLY
                            acodec.isAudioCodec() && !vcodec.isVideoCodec() -> FormatCategory.AUDIO_ONLY
                            else -> FormatCategory.OTHER
                        }
                        val size = readNumber(format, "filesize", "filesizeApprox", "filesize_approx")
                            ?.takeIf { it > 0 }
                            ?.let(::formatBytes)
                        FormatOption(id, ext, resolution, size, category, format)
                    }.distinctBy { it.id }.sortedWith(
                        compareBy<FormatOption> { it.category.ordinal }
                            .thenByDescending { it.resolution?.filter(Char::isDigit)?.toIntOrNull() ?: 0 }
                    )
                    status = if (formats.isEmpty()) {
                        "Metadata loaded, but no usable format entries were returned."
                    } else {
                        "${formats.size} usable formats found. Open the picker to inspect available options."
                    }
                    Log.i("VidamaExtractor", "Metadata loaded: ${formats.size} formats")
                }.onFailure { error ->
                    title = "Metadata query failed"
                    duration = null
                    formats = emptyList()
                    status = error.message ?: error.javaClass.simpleName
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

@Composable
private fun SandboxScreen(
    title: String,
    duration: String?,
    status: String,
    loading: Boolean,
    formats: List<FormatOption>,
    showPicker: Boolean,
    onTestMetadata: () -> Unit,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onFormatSelected: (FormatOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("VidamaCraft Extractor Sandbox", style = MaterialTheme.typography.headlineSmall)
        Text(title, style = MaterialTheme.typography.titleMedium)
        duration?.let { Text("Duration: $it") }
        Text(status, style = MaterialTheme.typography.bodyMedium)
        if (loading) {
            CircularProgressIndicator()
        }
        Button(onClick = onTestMetadata, enabled = !loading) {
            Text(if (loading) "Loading metadata…" else "Test metadata")
        }
        if (formats.isNotEmpty()) {
            OutlinedButton(onClick = onOpenPicker) {
                Text("Choose quality • ${formats.size} formats")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Sandbox only: selecting a format does not start a download.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissPicker,
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select video quality", style = MaterialTheme.typography.headlineSmall)
                Text("Choose an available stream. Estimated sizes are shown only when supplied by the extractor.")
                FormatCategory.values().forEach { category ->
                    val group = formats.filter { it.category == category }
                    if (group.isNotEmpty()) {
                        Text(category.label, style = MaterialTheme.typography.titleMedium)
                        group.forEach { option ->
                            FormatRow(option = option, onClick = { onFormatSelected(option) })
                        }
                        Divider()
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FormatRow(option: FormatOption, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(option.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Format ID: ${option.id}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            RadioButton(selected = false, onClick = onClick)
        }
    }
}

private fun Any.readProperty(vararg names: String): Any? {
    for (name in names) {
        val getterNames = listOf("get" + name.replaceFirstChar { it.uppercase() }, name)
        for (getterName in getterNames) {
            runCatching {
                javaClass.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
                    ?.invoke(this)
            }.getOrNull()?.let { return it }
        }
        var type: Class<*>? = javaClass
        while (type != null) {
            val field = runCatching { type!!.getDeclaredField(name) }.getOrNull()
            if (field != null) {
                val value = runCatching {
                    field.isAccessible = true
                    field.get(this)
                }.getOrNull()
                if (value != null) return value
            }
            type = type.superclass
        }
    }
    return null
}

private fun readString(value: Any, vararg names: String): String? =
    value.readProperty(*names)?.toString()?.takeIf { it.isNotBlank() && it != "null" }

private fun readNumber(value: Any, vararg names: String): Long? =
    value.readProperty(*names)?.toString()?.toDoubleOrNull()?.toLong()

private fun String?.isVideoCodec(): Boolean =
    !this.isNullOrBlank() && !equals("none", ignoreCase = true) && !equals("null", ignoreCase = true)

private fun String?.isAudioCodec(): Boolean =
    !this.isNullOrBlank() && !equals("none", ignoreCase = true) && !equals("null", ignoreCase = true)

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> String.format(Locale.ROOT, "%.1f GB", bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> String.format(Locale.ROOT, "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format(Locale.ROOT, "%.0f KB", bytes / 1_000.0)
    else -> "$bytes B"
}

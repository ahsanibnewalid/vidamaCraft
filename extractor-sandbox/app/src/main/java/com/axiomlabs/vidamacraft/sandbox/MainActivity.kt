package com.axiomlabs.vidamacraft.sandbox

import android.app.Activity
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.util.Locale
import java.util.concurrent.Executors

private val VidamaColors = darkColorScheme(
    primary = Color(0xFF42E8F5),
    onPrimary = Color(0xFF080C18),
    secondary = Color(0xFF9A75FF),
    background = Color(0xFF080C18),
    surface = Color(0xFF11182A),
    surfaceVariant = Color(0xFF1A2439),
    onSurfaceVariant = Color(0xFF9BA8BF)
)

private data class FormatOption(
    val id: String,
    val extension: String,
    val resolution: String?,
    val estimatedSize: String?,
    val bitrate: String?,
    val category: FormatCategory
) {
    val qualityLabel: String
        get() = resolution ?: if (category == FormatCategory.AUDIO_ONLY) "Audio" else "Format $id"

    val details: String
        get() = listOfNotNull(
            extension.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT),
            bitrate,
            estimatedSize?.let { "~$it" }
        ).joinToString(" • ").ifBlank { "Size not provided" }
}

private enum class FormatCategory(val label: String) {
    VIDEO_AUDIO("Video with audio"),
    VIDEO_ONLY("Video only"),
    AUDIO_ONLY("Audio"),
    OTHER("Other formats")
}

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()

    private var inputUrl by mutableStateOf("https://vimeo.com/22439234")
    private var loading by mutableStateOf(false)
    private var title by mutableStateOf("Metadata-only extractor sandbox")
    private var thumbnailUrl by mutableStateOf<String?>(null)
    private var duration by mutableStateOf<String?>(null)
    private var status by mutableStateOf("Tap Test metadata to query the authorized sample URL.")
    private var formats by mutableStateOf<List<FormatOption>>(emptyList())
    private var showPicker by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = VidamaColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SandboxScreen(
                        inputUrl = inputUrl,
                        onUrlChange = { inputUrl = it },
                        onPaste = ::pasteUrlFromClipboard,
                        title = title,
                        thumbnailUrl = thumbnailUrl,
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
                                "Selected ${option.qualityLabel} (${option.details}). Download is not wired up yet.",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.i("VidamaExtractor", "Selected format id=${option.id}, category=${option.category}")
                        }
                    )
                }
            }
        }
    }

    private fun pasteUrlFromClipboard() {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val pasted = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (pasted.isNullOrBlank()) {
            Toast.makeText(this, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
        } else {
            inputUrl = pasted
        }
    }

    private fun queryMetadata() {
        if (loading) return
        // Metadata-only experiment: test only URLs you are authorized to access.
        val testUrl = inputUrl.trim()
        val parsed = runCatching { Uri.parse(testUrl) }.getOrNull()
        if (parsed == null || parsed.host.isNullOrBlank() ||
            !(parsed.scheme.equals("https", true) || parsed.scheme.equals("http", true))
        ) {
            status = "Enter a valid HTTP or HTTPS URL."
            formats = emptyList()
            return
        }
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
                    thumbnailUrl = info.thumbnail
                    duration = info.duration?.let(::formatDuration)
                    formats = info.formats.orEmpty().mapNotNull { format ->
                        val id = format.formatId?.toString()?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        val ext = format.ext?.toString().orEmpty()
                        val height = readNumber(format, "height")?.toInt()
                        val note = readString(format, "resolution", "formatNote", "format_note")
                        val resolution = when {
                            height != null && height > 0 -> "${height}p"
                            !note.isNullOrBlank() -> Regex("(?i)([0-9]{3,4}p)").find(note)?.value
                            else -> null
                        }
                        val vcodec = readString(format, "vcodec", "videoCodec", "video_codec")
                        val acodec = readString(format, "acodec", "audioCodec", "audio_codec")
                        val category = when {
                            vcodec.isVideoCodec() && acodec.isAudioCodec() -> FormatCategory.VIDEO_AUDIO
                            vcodec.isVideoCodec() -> FormatCategory.VIDEO_ONLY
                            acodec.isAudioCodec() -> FormatCategory.AUDIO_ONLY
                            else -> FormatCategory.OTHER
                        }
                        val size = readNumber(format, "filesize", "filesizeApprox", "filesize_approx")
                            ?.takeIf { it > 0 }
                            ?.let(::formatBytes)
                        val bitrate = readNumber(format, "abr", "tbr", "bitrate")
                            ?.takeIf { it > 0 }
                            ?.let { "${it} kbps" }
                        FormatOption(id, ext, resolution, size, bitrate, category)
                    }.distinctBy { it.id }.sortedWith(
                        compareBy<FormatOption> { it.category.ordinal }
                            .thenByDescending { it.resolution?.filter(Char::isDigit)?.toIntOrNull() ?: 0 }
                            .thenBy { it.extension }
                    )
                    status = if (formats.isEmpty()) {
                        "Metadata loaded, but no usable format entries were returned."
                    } else {
                        "${formats.size} formats available. Choose a stream to inspect its details."
                    }
                    Log.i("VidamaExtractor", "Metadata loaded: ${formats.size} formats")
                }.onFailure { error ->
                    title = "Metadata query failed"
                    thumbnailUrl = null
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
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    title: String,
    thumbnailUrl: String?,
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
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF080C18), Color(0xFF15152D), Color(0xFF080C18))))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("VidamaCraft", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("EXTRACTOR SANDBOX • METADATA ONLY", style = MaterialTheme.typography.labelSmall, color = Color(0xFF42E8F5))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF11182A))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF42E8F5))
                    Text("Media URL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onPaste) { Text("Paste") }
                }
                TextField(
                    value = inputUrl,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://…") },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF080C18),
                        unfocusedContainerColor = Color(0xFF080C18),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Color(0xFF42E8F5)
                    )
                )
            }
        }
        VideoHeaderCard(title = title, thumbnailUrl = thumbnailUrl, duration = duration)
        Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (loading) CircularProgressIndicator()
        Button(onClick = onTestMetadata, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) "Loading metadata…" else "Test metadata")
        }
        if (formats.isNotEmpty()) {
            OutlinedButton(onClick = onOpenPicker, modifier = Modifier.fillMaxWidth()) {
                Text("Choose quality • ${formats.size} formats")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Experimental sandbox only. Selecting a format does not start a download. Use content you are authorized to access; DRM and access controls are not bypassed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = onDismissPicker, sheetState = sheetState) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Choose your quality", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF42E8F5))
                Text(
                    "Available streams are supplied by the extractor. Sizes and bitrates may be estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FormatCategory.values().forEach { category ->
                        val group = formats.filter { it.category == category }
                        if (group.isNotEmpty()) {
                            item(key = "heading-${category.name}") {
                                Text(
                                    category.label,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            items(group, key = { "${category.name}-${it.id}" }) { option ->
                                FormatRow(option = option, onClick = { onFormatSelected(option) })
                            }
                        }
                    }
                }
                TextButton(onClick = onDismissPicker, modifier = Modifier.align(Alignment.End)) {
                    Text("Cancel")
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun VideoHeaderCard(title: String, thumbnailUrl: String?, duration: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(112.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Video thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 3)
                Text("Select a format", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                duration?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun FormatRow(option: FormatOption, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    option.qualityLabel,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(option.extension.uppercase(Locale.ROOT).ifBlank { "MEDIA" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(option.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Format ID: ${option.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Download, contentDescription = "Select format", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun Any.readProperty(vararg names: String): Any? {
    for (name in names) {
        val getterNames = listOf("get" + name.replaceFirstChar { it.uppercase() }, name)
        for (getterName in getterNames) {
            runCatching {
                javaClass.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }?.invoke(this)
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

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remainingSeconds = seconds % 60
    return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, remainingSeconds)
    else String.format(Locale.ROOT, "%d:%02d", minutes, remainingSeconds)
}

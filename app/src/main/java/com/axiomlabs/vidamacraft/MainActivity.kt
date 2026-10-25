package com.axiomlabs.vidamacraft

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.axiomlabs.vidamacraft.data.local.DownloadStatus
import com.axiomlabs.vidamacraft.ui.downloads.DownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { DownloaderScreen(this) } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloaderScreen(context: Context, viewModel: DownloadViewModel = hiltViewModel()) {
    var url by remember { mutableStateOf("") }
    var filename by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Paste a direct HTTPS file link to get started.") }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.VideoLibrary, contentDescription = null)
                Text("VidamaCraft")
            }
        })
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Your media. Your downloads.", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text("Download direct files you own or are authorized to save. Streaming-page links may not work.")
            }
            item {
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Direct HTTPS URL") }, placeholder = { Text("https://example.com/video.mp4") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = filename, onValueChange = { filename = it }, label = { Text("File name (optional)") }, placeholder = { Text("my-video.mp4") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(12.dp))
                Button(enabled = !uiState.isSubmitting, onClick = {
                    val validation = DownloadUrlValidator.validate(url)
                    if (validation != null) {
                        message = validation
                    } else {
                        val sourceUrl = url.trim()
                        val safeName = safeFilename(filename.ifBlank { nameFromUrl(sourceUrl) })
                        viewModel.queueDownload(sourceUrl, safeName) { recordId ->
                            try {
                                val request = DownloadManager.Request(Uri.parse(sourceUrl))
                                    .setTitle(safeName)
                                    .setDescription("VidamaCraft download")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setAllowedOverMetered(true)
                                    .setAllowedOverRoaming(false)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
                                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                val systemDownloadId = manager.enqueue(request)
                                viewModel.setSystemDownloadId(recordId, systemDownloadId)
                                viewModel.setStatus(recordId, DownloadStatus.DOWNLOADING)
                                url = ""
                                filename = ""
                                message = "Download queued. Progress will sync automatically."
                            } catch (_: Exception) {
                                viewModel.setStatus(recordId, DownloadStatus.FAILED, "Android could not start the download.")
                                message = "Could not start this download. Check the URL and try again."
                            }
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text(if (uiState.isSubmitting) "  Saving…" else "  Download file")
                }
                Text(uiState.message ?: message, style = MaterialTheme.typography.bodySmall)
            }
            item { Text("Download history", style = MaterialTheme.typography.titleLarge) }
            if (uiState.downloads.isEmpty()) {
                item { Card { Text("Your saved downloads will appear here.", Modifier.padding(16.dp)) } }
            } else {
                items(uiState.downloads, key = { it.id }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.fileName, style = MaterialTheme.typography.titleMedium)
                            Text(item.status.name, style = MaterialTheme.typography.labelMedium)
                            item.errorMessage?.let { error ->
                                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            }
                            if (item.totalBytes > 0L) {
                                val progress = (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                                Text("${(progress * 100).toInt()}% · ${item.downloadedBytes} / ${item.totalBytes} bytes", style = MaterialTheme.typography.bodySmall)
                            } else {
                                if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                                Text("${item.downloadedBytes} bytes downloaded · total size unknown", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(item.url, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            Button(onClick = { viewModel.deleteDownload(item.id) }) { Text("Remove from history") }
                        }
                    }
                }
            }
            item { Text("VidamaCraft does not bypass DRM or access controls. Only download content you are allowed to save.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun safeFilename(raw: String): String {
    val cleaned = raw.replace(Regex("[^A-Za-z0-9._ -]"), "_").trim().trim('.').take(120)
    return cleaned.ifBlank { "download.bin" }
}

private fun nameFromUrl(raw: String): String {
    val path = Uri.parse(raw).lastPathSegment.orEmpty()
    val decoded = try { URLDecoder.decode(path, StandardCharsets.UTF_8.name()) } catch (_: Exception) { path }
    return safeFilename(decoded.ifBlank { "download.bin" })
}

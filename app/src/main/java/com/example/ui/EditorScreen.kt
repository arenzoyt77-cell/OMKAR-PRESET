package com.example.ui

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ClipsListCard
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.KeyframeInspector
import com.example.ui.components.TimelineTrack
import com.example.ui.components.VideoPreviewBox
import com.example.ui.components.WarningDialog
import com.example.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Display snackbar for status messages
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatus()
        }
    }

    // Document Creators / Pickers
    // 1. Create Document (.omkarpreset)
    val savePresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.savePresetToUri(context.contentResolver, uri)
        }
    }

    // 2. Open Document (.omkarpreset)
    val importPresetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPresetFromUri(context.contentResolver, uri)
        }
    }

    // 3. Photo Picker for Master Video (Play Policy compliant zero permission picker)
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val (durationMs, w, h) = extractVideoMetadata(context, uri)
            viewModel.setVideoSource(uri, durationMs, w, h)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "OMKAR PRESET",
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Video Editing Instruction Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    // Share Preset Native Intent
                    OutlinedButton(
                        onClick = {
                            val shareIntent = viewModel.createShareIntent(context)
                            context.startActivity(Intent.createChooser(shareIntent, "Share Omkar Preset"))
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("share_preset_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 700.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Preset Name & Primary Actions Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = uiState.presetName,
                        onValueChange = { viewModel.setPresetName(it) },
                        label = { Text("Preset Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_name_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                pickVideoLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_video_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Video", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val filename = "${uiState.presetName.replace(" ", "_")}.omkarpreset"
                                savePresetLauncher.launch(filename)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_preset_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Preset", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                importPresetLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_preset_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Preset", fontSize = 12.sp)
                        }
                    }

                    // Quick duration switcher for testing target duration mismatch rules
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Test Duration: ${uiState.videoDurationMs / 1000}s",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.setVideoSource(null, 5_000L) },
                                modifier = Modifier.height(30.dp)
                            ) { Text("5s", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.setVideoSource(null, 12_000L) },
                                modifier = Modifier.height(30.dp)
                            ) { Text("12s", fontSize = 10.sp) }
                            OutlinedButton(
                                onClick = { viewModel.setVideoSource(null, 25_000L) },
                                modifier = Modifier.height(30.dp)
                            ) { Text("25s", fontSize = 10.sp) }
                        }
                    }
                }
            }

            // Video Preview Canvas
            VideoPreviewBox(
                currentTransform = uiState.currentTransform,
                currentPositionMs = uiState.currentPositionMs,
                totalDurationMs = uiState.videoDurationMs,
                isPlaying = uiState.isPlaying,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onTransformChange = { px, py, z ->
                    if (uiState.selectedKeyframeId != null) {
                        viewModel.updateSelectedKeyframe(x = px, y = py, zoom = z)
                    } else {
                        viewModel.addKeyframeAtPlayhead(x = px, y = py, zoom = z)
                    }
                }
            )

            // Timeline Track with scrubber, splits, clips & diamonds
            TimelineTrack(
                currentPositionMs = uiState.currentPositionMs,
                totalDurationMs = uiState.videoDurationMs,
                splitPointsMs = uiState.splitPointsMs,
                clips = uiState.clips,
                keyframes = uiState.keyframes,
                selectedKeyframeId = uiState.selectedKeyframeId,
                onSeek = { viewModel.seekTo(it) },
                onAddSplit = { viewModel.addSplitAtPlayhead() },
                onAddKeyframe = { viewModel.addKeyframeAtPlayhead() },
                onSelectKeyframe = { viewModel.selectKeyframe(it) }
            )

            // Clips List
            ClipsListCard(
                clips = uiState.clips,
                currentPositionMs = uiState.currentPositionMs,
                onSeekToClip = { viewModel.seekTo(it) }
            )

            // Keyframe Inspector
            KeyframeInspector(
                selectedKeyframe = uiState.selectedKeyframe,
                onUpdateKeyframe = { x, y, z, m ->
                    viewModel.updateSelectedKeyframe(x = x, y = y, zoom = z, mode = m)
                },
                onDeleteKeyframe = { viewModel.deleteKeyframe(it) }
            )

            // Export Video Action
            Button(
                onClick = { viewModel.startExport() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_video_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rendering Pipeline (${(uiState.exportProgress * 100).toInt()}%)...")
                } else {
                    Icon(Icons.Default.Movie, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Preview & Export Video", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dialogs
        uiState.warningDialogData?.let { warningData ->
            WarningDialog(
                data = warningData,
                onKeepExact = { viewModel.confirmImport(keepOutOfRange = true) },
                onClampToDuration = { viewModel.confirmImport(keepOutOfRange = false) },
                onDismiss = { viewModel.dismissWarning() }
            )
        }

        uiState.exportReport?.let { report ->
            ExportReportDialog(
                report = report,
                onDismiss = { viewModel.dismissExportReport() }
            )
        }
    }
}

private fun extractVideoMetadata(context: Context, uri: Uri): Triple<Long, Int, Int> {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        retriever.release()

        val dur = durationStr?.toLongOrNull() ?: 10_000L
        val w = widthStr?.toIntOrNull() ?: 1920
        val h = heightStr?.toIntOrNull() ?: 1080
        Triple(dur, w, h)
    } catch (e: Exception) {
        Triple(10_000L, 1920, 1080)
    }
}

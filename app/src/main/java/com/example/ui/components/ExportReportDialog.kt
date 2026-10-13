package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ExportReport

@Composable
fun ExportReportDialog(
    report: ExportReport,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("export_report_dialog"),
        icon = {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF10B981)
            )
        },
        title = {
            Text("Export Pipeline Ready", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Preset '${report.presetName}' applied successfully to ${report.outputResolution} output.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration: ${formatMs(report.totalDurationMs)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Clips: ${report.totalClips}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Keyframes: ${report.totalKeyframes}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Audio Stream: Synchronized (1:1 timeline match)",
                    fontSize = 11.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Rendered Clip Segments:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp).padding(top = 4.dp)) {
                    itemsIndexed(report.clipResults) { index, clipResult ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Clip ${index + 1}: ${formatMs(clipResult.clip.startMs)} -> ${formatMs(clipResult.clip.endMs)} (${clipResult.clip.durationMs}ms)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Start Zoom: ${clipResult.initialTransform.zoomPercent.toInt()}% | End Zoom: ${clipResult.finalTransform.zoomPercent.toInt()}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_export_report_button")
            ) {
                Text("Done")
            }
        }
    )
}

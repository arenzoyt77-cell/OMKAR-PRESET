package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.WarningDialogData

@Composable
fun WarningDialog(
    data: WarningDialogData,
    onKeepExact: () -> Unit,
    onClampToDuration: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("out_of_range_warning_dialog"),
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(data.title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = data.message,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Affected elements:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                LazyColumn(modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 4.dp)) {
                    if (data.outOfRangeSplits.isNotEmpty()) {
                        item {
                            Text("Splits exceeding ${data.targetDurationMs}ms:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        items(data.outOfRangeSplits) { split ->
                            Text(" • Split at ${split}ms (+${split - data.targetDurationMs}ms)", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }

                    if (data.outOfRangeKeyframes.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Keyframes exceeding ${data.targetDurationMs}ms:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        items(data.outOfRangeKeyframes) { kf ->
                            Text(" • Keyframe at ${kf.timestampMs}ms (Zoom: ${kf.zoomPercent.toInt()}%)", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onKeepExact,
                modifier = Modifier.testTag("keep_exact_timestamps_button")
            ) {
                Text("Keep Exact Timestamps")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onClampToDuration,
                modifier = Modifier.testTag("clamp_to_duration_button")
            ) {
                Text("Clamp to Video")
            }
        }
    )
}

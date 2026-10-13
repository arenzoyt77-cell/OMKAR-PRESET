package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InterpolationMode
import com.example.model.PresetKeyframe

@Composable
fun KeyframeInspector(
    selectedKeyframe: PresetKeyframe?,
    onUpdateKeyframe: (x: Float?, y: Float?, zoom: Float?, mode: InterpolationMode?) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("keyframe_inspector"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "KEYFRAME TRANSFORM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedKeyframe != null) {
                        Text(
                            text = "Timestamp: ${formatMs(selectedKeyframe.timestampMs)} (${selectedKeyframe.timestampMs} ms)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "No keyframe selected (scrub or tap diamond)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (selectedKeyframe != null) {
                    IconButton(
                        onClick = { onDeleteKeyframe(selectedKeyframe.id) },
                        modifier = Modifier.testTag("delete_keyframe_button")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Keyframe",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (selectedKeyframe != null) {
                Spacer(modifier = Modifier.height(10.dp))

                // Zoom Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Zoom", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${selectedKeyframe.zoomPercent.toInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = selectedKeyframe.zoomPercent,
                    onValueChange = { onUpdateKeyframe(null, null, it, null) },
                    valueRange = 50f..400f,
                    modifier = Modifier.testTag("zoom_slider")
                )

                // Pan X Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pan X", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${selectedKeyframe.x.toInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = selectedKeyframe.x,
                    onValueChange = { onUpdateKeyframe(it, null, null, null) },
                    valueRange = -100f..100f,
                    modifier = Modifier.testTag("pan_x_slider")
                )

                // Pan Y Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pan Y", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${selectedKeyframe.y.toInt()}%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                Slider(
                    value = selectedKeyframe.y,
                    onValueChange = { onUpdateKeyframe(null, it, null, null) },
                    valueRange = -100f..100f,
                    modifier = Modifier.testTag("pan_y_slider")
                )

                // Interpolation Mode Selector
                Text(
                    "Interpolation Mode",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InterpolationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedKeyframe.interpolationMode == mode,
                            onClick = { onUpdateKeyframe(null, null, null, mode) },
                            label = { Text(mode.name, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

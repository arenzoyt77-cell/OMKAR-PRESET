package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.InterpolationMode
import com.example.model.PresetClip
import com.example.model.PresetKeyframe

@Composable
fun TimelineTrack(
    currentPositionMs: Long,
    totalDurationMs: Long,
    splitPointsMs: List<Long>,
    clips: List<PresetClip>,
    keyframes: List<PresetKeyframe>,
    selectedKeyframeId: String?,
    onSeek: (Long) -> Unit,
    onAddSplit: () -> Unit,
    onAddKeyframe: () -> Unit,
    onSelectKeyframe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("timeline_track_container"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: quick action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TIMELINE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${formatMs(currentPositionMs)} (${currentPositionMs} ms)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAddSplit,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("add_split_button")
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Add Split", modifier = Modifier.height(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Split", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onAddKeyframe,
                        modifier = Modifier.testTag("add_keyframe_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Keyframe", modifier = Modifier.height(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Keyframe", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Clips ribbon view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val clipColors = listOf(
                    Color(0xFF0284C7),
                    Color(0xFF0D9488),
                    Color(0xFF4F46E5),
                    Color(0xFF7C3AED),
                    Color(0xFFC026D3)
                )

                clips.forEachIndexed { idx, clip ->
                    val weight = (clip.durationMs.toFloat() / totalDurationMs.coerceAtLeast(1L).toFloat()).coerceAtLeast(0.01f)
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(24.dp)
                            .background(clipColors[idx % clipColors.size])
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = clip.name.ifBlank { "C${idx + 1}" },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Interactive Scrubber Track (Splits & Keyframes)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .pointerInput(totalDurationMs) {
                        detectTapGestures { offset ->
                            val progress = (offset.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (progress * totalDurationMs).toLong()
                            onSeek(targetMs)
                        }
                    }
                    .pointerInput(totalDurationMs) {
                        detectDragGestures { change, _ ->
                            val progress = (change.position.x / size.width).coerceIn(0f, 1f)
                            val targetMs = (progress * totalDurationMs).toLong()
                            onSeek(targetMs)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                    val w = size.width
                    val h = size.height
                    val duration = totalDurationMs.coerceAtLeast(1L)

                    // Draw time ticks (every 1 second)
                    val secondCount = (duration / 1000L).toInt()
                    for (sec in 0..secondCount) {
                        val tickMs = sec * 1000L
                        val x = (tickMs.toFloat() / duration.toFloat()) * w
                        drawLine(
                            color = Color(0x4494A3B8),
                            start = Offset(x, 0f),
                            end = Offset(x, if (sec % 5 == 0) 14f else 8f),
                            strokeWidth = 1.5f
                        )
                    }

                    // Draw Split Cut lines (vertical bright lines)
                    splitPointsMs.forEach { splitMs ->
                        val x = (splitMs.toFloat() / duration.toFloat()) * w
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 3f
                        )
                    }

                    // Draw Keyframe Diamonds on the track
                    keyframes.forEach { kf ->
                        val kfX = (kf.timestampMs.toFloat() / duration.toFloat()) * w
                        val kfY = h * 0.7f
                        val isSelected = kf.id == selectedKeyframeId
                        val diamondRadius = if (isSelected) 10f else 7f

                        val diamondPath = Path().apply {
                            moveTo(kfX, kfY - diamondRadius)
                            lineTo(kfX + diamondRadius, kfY)
                            lineTo(kfX, kfY + diamondRadius)
                            lineTo(kfX - diamondRadius, kfY)
                            close()
                        }

                        val diamondColor = when {
                            isSelected -> Color(0xFFFACC15) // Bright yellow for selected
                            kf.interpolationMode == InterpolationMode.HOLD -> Color(0xFFF97316)
                            kf.interpolationMode == InterpolationMode.EASE_IN_OUT -> Color(0xFFA855F7)
                            else -> Color(0xFF38BDF8)
                        }

                        drawPath(path = diamondPath, color = diamondColor)
                    }

                    // Draw Playhead line & head
                    val playheadX = (currentPositionMs.toFloat() / duration.toFloat()) * w
                    drawLine(
                        color = Color.White,
                        start = Offset(playheadX, 0f),
                        end = Offset(playheadX, h),
                        strokeWidth = 2.5f
                    )

                    // Playhead marker top cap
                    drawCircle(
                        color = Color.White,
                        radius = 6f,
                        center = Offset(playheadX, 6f)
                    )
                }
            }
        }
    }
}

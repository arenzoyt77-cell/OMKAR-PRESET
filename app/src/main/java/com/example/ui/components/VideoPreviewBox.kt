package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TransformEvaluation

@Composable
fun VideoPreviewBox(
    currentTransform: TransformEvaluation,
    currentPositionMs: Long,
    totalDurationMs: Long,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onTransformChange: (panX: Float, panY: Float, zoom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .testTag("video_preview_box")
    ) {
        // Video canvas simulating transformed viewport
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomFactor, _ ->
                        val newZoom = (currentTransform.zoomPercent * zoomFactor).coerceIn(50f, 500f)
                        val newX = (currentTransform.x + pan.x * 0.1f).coerceIn(-100f, 100f)
                        val newY = (currentTransform.y + pan.y * 0.1f).coerceIn(-100f, 100f)
                        onTransformChange(newX, newY, newZoom)
                    }
                }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            val scale = currentTransform.effectiveScale
            // Pan offset converted to canvas pixels
            val panOffsetX = (currentTransform.x / 100f) * (size.width * 0.4f)
            val panOffsetY = (currentTransform.y / 100f) * (size.height * 0.4f)

            // Simulated video content frame
            val baseW = size.width * 0.85f
            val baseH = size.height * 0.85f
            val transformedW = baseW * scale
            val transformedH = baseH * scale

            val left = cx - (transformedW / 2f) + panOffsetX
            val top = cy - (transformedH / 2f) + panOffsetY

            // Background frame shadow
            drawRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(transformedW, transformedH)
            )

            // Decorative scene simulation (gradient horizon & sun to show transform clearly)
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 28f * scale,
                center = Offset(left + transformedW * 0.5f, top + transformedH * 0.45f)
            )

            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(left, top + transformedH * 0.65f),
                end = Offset(left + transformedW, top + transformedH * 0.65f),
                strokeWidth = 3f * scale
            )

            // Rule of thirds guide lines
            val thirdX = size.width / 3f
            val thirdY = size.height / 3f
            val gridColor = Color(0x33FFFFFF)
            val strokeDashed = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))

            drawLine(color = gridColor, start = Offset(thirdX, 0f), end = Offset(thirdX, size.height), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(thirdX * 2f, 0f), end = Offset(thirdX * 2f, size.height), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, thirdY), end = Offset(size.width, thirdY), strokeWidth = 1f)
            drawLine(color = gridColor, start = Offset(0f, thirdY * 2f), end = Offset(size.width, thirdY * 2f), strokeWidth = 1f)

            // Outer viewport frame
            drawRect(
                color = Color(0xFF64748B),
                topLeft = Offset.Zero,
                size = size,
                style = Stroke(width = 2f)
            )
        }

        // Top badges: Transform stats & timecode
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xCC000000),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "ZOOM: ${currentTransform.zoomPercent.toInt()}%  |  PAN: (${currentTransform.x.toInt()}, ${currentTransform.y.toInt()})",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            color = Color(0xCC000000),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "${formatMs(currentPositionMs)} / ${formatMs(totalDurationMs)}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Bottom center: Play/Pause button
        FilledIconButton(
            onClick = onTogglePlayPause,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .testTag("play_pause_button"),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
    }
}

fun formatMs(ms: Long): String {
    val totalSec = ms / 1000L
    val min = totalSec / 60L
    val sec = totalSec % 60L
    val millis = ms % 1000L
    return "%02d:%02d.%03d".format(min, sec, millis)
}

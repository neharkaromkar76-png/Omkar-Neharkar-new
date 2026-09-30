package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EasingType
import com.example.data.model.Keyframe
import com.example.data.model.MotionType
import com.example.engine.motion.Interpolator
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioAmberGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import java.util.UUID

@Composable
fun KeyframeTimelineView(
    keyframes: List<Keyframe>,
    currentTimeMs: Long,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    onAddKeyframe: (Keyframe) -> Unit,
    onUpdateKeyframe: (Keyframe) -> Unit,
    onDeleteKeyframe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = maxOf(1000L, totalDurationMs)
    var selectedKeyframeId by remember { mutableStateOf<String?>(null) }
    val selectedKeyframe = keyframes.find { it.id == selectedKeyframeId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        // Timeline Header: Keyframe Count & Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = StudioAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Keyframe Track",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = StudioTextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(StudioSurfaceHighlight, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${keyframes.size} nodes",
                        fontSize = 11.sp,
                        color = StudioTextSecondary
                    )
                }
            }

            // Quick Add at current Playhead
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val currentTransform = Interpolator.evaluateKeyframeAtTime(currentTimeMs, keyframes)
                        val newKf = Keyframe(
                            id = UUID.randomUUID().toString(),
                            timestampMs = currentTimeMs,
                            x = currentTransform.x,
                            y = currentTransform.y,
                            scale = currentTransform.scale,
                            rotation = currentTransform.rotation,
                            easing = EasingType.SMOOTH,
                            motionType = MotionType.COMBINED
                        )
                        onAddKeyframe(newKf)
                        selectedKeyframeId = newKf.id
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StudioAmber,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("timeline_add_keyframe_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Node", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (selectedKeyframe != null) {
                    OutlinedButton(
                        onClick = {
                            onDeleteKeyframe(selectedKeyframe.id)
                            selectedKeyframeId = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StudioRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("timeline_delete_keyframe_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Visual Graph & Keyframe Track Canvas
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090D14))
                .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
        ) {
            val trackWidthPx = constraints.maxWidth.toFloat()
            val trackHeightPx = constraints.maxHeight.toFloat()

            // 1. Draw Background Grid, Time Markers & Interpolation Curve
            Canvas(modifier = Modifier.fillMaxSize().pointerInput(duration) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    val targetMs = (fraction * duration).toLong()
                    onSeek(targetMs)
                }
            }) {
                val w = size.width
                val h = size.height

                // Grid lines every 10%
                for (i in 1..9) {
                    val x = (i / 10f) * w
                    drawLine(
                        color = Color(0xFF1E2838),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Interpolation motion curve (draw scale line)
                if (keyframes.isNotEmpty()) {
                    val path = Path()
                    val stepSamples = 100
                    for (step in 0..stepSamples) {
                        val sampleTime = (step.toFloat() / stepSamples * duration).toLong()
                        val sampleTransform = Interpolator.evaluateKeyframeAtTime(sampleTime, keyframes)
                        val sampleX = (sampleTime.toFloat() / duration) * w
                        // Map scale 1.0..2.5 to height
                        val normScale = ((sampleTransform.scale - 1.0f) / 1.5f).coerceIn(0f, 1f)
                        val sampleY = h - (normScale * (h - 24f) + 12f)

                        if (step == 0) {
                            path.moveTo(sampleX, sampleY)
                        } else {
                            path.lineTo(sampleX, sampleY)
                        }
                    }
                    drawPath(
                        path = path,
                        color = StudioCyan.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // 2. Keyframe Diamond Markers
            keyframes.forEach { kf ->
                val fraction = (kf.timestampMs.toFloat() / duration).coerceIn(0f, 1f)
                val isSelected = kf.id == selectedKeyframeId

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (trackWidthPx * fraction).toInt() - 14,
                                y = (trackHeightPx / 2).toInt() - 14
                            )
                        }
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) StudioAmber.copy(alpha = 0.35f) else Color.Transparent)
                        .clickable {
                            selectedKeyframeId = kf.id
                            onSeek(kf.timestampMs)
                        }
                        .testTag("keyframe_marker_${kf.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Keyframe at ${kf.formattedTime}",
                        tint = if (isSelected) StudioCyan else StudioAmber,
                        modifier = Modifier.size(if (isSelected) 22.dp else 16.dp)
                    )
                }
            }

            // 3. Playhead Vertical Scrubber Line
            val playheadFraction = (currentTimeMs.toFloat() / duration).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (trackWidthPx * playheadFraction).toInt() - 1,
                            y = 0
                        )
                    }
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )
        }

        // Timecode scale labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0.0s", fontSize = 10.sp, color = StudioTextSecondary, fontFamily = FontFamily.Monospace)
            Text(text = String.format("%.1fs", duration / 2000f), fontSize = 10.sp, color = StudioTextSecondary, fontFamily = FontFamily.Monospace)
            Text(text = String.format("%.1fs", duration / 1000f), fontSize = 10.sp, color = StudioTextSecondary, fontFamily = FontFamily.Monospace)
        }

        // Selected Keyframe Property Inspector
        AnimatedVisibility(visible = selectedKeyframe != null) {
            if (selectedKeyframe != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceHighlight)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Keyframe at ${selectedKeyframe.formattedTime}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = StudioCyan
                        )

                        // Easing Selector Dropdown
                        var easingMenuOpen by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioSurfaceElevated)
                                    .clickable { easingMenuOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Easing: ${selectedKeyframe.easing.displayName}",
                                    fontSize = 11.sp,
                                    color = StudioAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            DropdownMenu(
                                expanded = easingMenuOpen,
                                onDismissRequest = { easingMenuOpen = false }
                            ) {
                                EasingType.values().forEach { easing ->
                                    DropdownMenuItem(
                                        text = { Text(easing.displayName) },
                                        onClick = {
                                            onUpdateKeyframe(selectedKeyframe.copy(easing = easing))
                                            easingMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Zoom / Scale Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format("Zoom: %.2fx", selectedKeyframe.scale),
                            fontSize = 11.sp,
                            color = StudioTextSecondary,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = selectedKeyframe.scale,
                            onValueChange = { onUpdateKeyframe(selectedKeyframe.copy(scale = it)) },
                            valueRange = 1.0f..2.8f,
                            colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Rotation Slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format("Rot: %.1f°", selectedKeyframe.rotation),
                            fontSize = 11.sp,
                            color = StudioTextSecondary,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = selectedKeyframe.rotation,
                            onValueChange = { onUpdateKeyframe(selectedKeyframe.copy(rotation = it)) },
                            valueRange = -25f..25f,
                            colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Normalized Center Pan X & Y
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format("X Pos: %.2f", selectedKeyframe.x),
                            fontSize = 11.sp,
                            color = StudioTextSecondary,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = selectedKeyframe.x,
                            onValueChange = { onUpdateKeyframe(selectedKeyframe.copy(x = it)) },
                            valueRange = 0.15f..0.85f,
                            colors = SliderDefaults.colors(thumbColor = StudioTextPrimary, activeTrackColor = StudioTextPrimary),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

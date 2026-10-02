package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOn
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubjectRegion
import com.example.data.model.VideoMetadata
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary

enum class SplitLayoutMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SIDE_BY_SIDE("Side-by-Side", Icons.Default.Splitscreen),
    STACKED("Stacked", Icons.Default.ViewAgenda),
    WIPE_SLIDER("Wipe Comparison", Icons.Default.Compare)
}

/**
 * Split-screen comparison layout that displays the reference video side-by-side
 * with the user's footage, equipped with custom playback controls (play, pause, seek, loop,
 * speed, and frame-by-frame stepping) for micro-level visual analysis by both the user and AI.
 */
@Composable
fun SplitScreenComparisonLayout(
    referenceBitmap: Bitmap?,
    referenceMetadata: VideoMetadata?,
    targetBitmap: Bitmap?,
    targetMetadata: VideoMetadata?,
    targetSubject: SubjectRegion?,
    currentTimeMs: Long,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onStartAnalysis: () -> Unit,
    modifier: Modifier = Modifier,
    isLooping: Boolean = true,
    onToggleLoop: () -> Unit = {},
    playbackSpeed: Float = 1.0f,
    onSpeedChange: (Float) -> Unit = {},
    onStepFrameForward: () -> Unit = {},
    onStepFrameBackward: () -> Unit = {},
    onJumpSeconds: (Float) -> Unit = {},
    isAnalyzing: Boolean = false,
    analysisProgress: Float = 0f,
    analysisStageMessage: String = ""
) {
    var splitMode by remember { mutableStateOf(SplitLayoutMode.SIDE_BY_SIDE) }
    var wipeFraction by remember { mutableFloatStateOf(0.5f) }
    var showSubjectGuide by remember { mutableStateOf(true) }

    val refDuration = referenceMetadata?.durationMs ?: 8000L
    val targetDuration = targetMetadata?.durationMs ?: 14000L
    val targetFps = targetMetadata?.fps ?: 30f
    val refFps = referenceMetadata?.fps ?: 60f

    val targetFrameCurrent = ((currentTimeMs * targetFps) / 1000f).toInt() + 1
    val targetFrameTotal = ((targetDuration * targetFps) / 1000f).toInt().coerceAtLeast(1)

    val mappedRefTime = if (targetDuration > 0) {
        (currentTimeMs.toFloat() / targetDuration.toFloat() * refDuration).toLong().coerceIn(0L, refDuration)
    } else currentTimeMs.coerceIn(0L, refDuration)

    val refFrameCurrent = ((mappedRefTime * refFps) / 1000f).toInt() + 1
    val refFrameTotal = ((refDuration * refFps) / 1000f).toInt().coerceAtLeast(1)

    CinematicCard(
        modifier = modifier.testTag("split_screen_comparison_layout"),
        borderColor = StudioCyan.copy(alpha = 0.35f),
        backgroundColor = StudioSurface
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Top Toolbar: Mode Switcher & Guide Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Layout Mode Selector
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceHighlight)
                        .padding(2.dp)
                ) {
                    SplitLayoutMode.values().forEach { mode ->
                        val selected = mode == splitMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) StudioPrimary else Color.Transparent)
                                .clickable { splitMode = mode }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("split_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.label,
                                    tint = if (selected) Color.White else StudioTextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = mode.label,
                                    color = if (selected) Color.White else StudioTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Guide toggle button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSubjectGuide = !showSubjectGuide }
                        .background(if (showSubjectGuide) StudioEmerald.copy(alpha = 0.15f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("toggle_subject_guide_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Subject Guide",
                        tint = if (showSubjectGuide) StudioEmerald else StudioTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Subject Guide",
                        color = if (showSubjectGuide) StudioEmerald else StudioTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Split Viewport Container
            when (splitMode) {
                SplitLayoutMode.SIDE_BY_SIDE -> {
                    // Side-by-side dual columns with 16:9 box container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Left Pane: Reference Video
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF0F172A))
                                .testTag("split_reference_pane")
                        ) {
                            VideoPaneContent(
                                bitmap = referenceBitmap,
                                label = "REFERENCE MOTION",
                                badgeColor = StudioAmber,
                                metadata = referenceMetadata,
                                currentFrame = refFrameCurrent,
                                totalFrames = refFrameTotal,
                                currentTimeMs = mappedRefTime,
                                totalDurationMs = refDuration,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Center Divider Line
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(StudioCyan.copy(alpha = 0.6f))
                        )

                        // Right Pane: User's Footage
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color(0xFF0F172A))
                                .testTag("split_target_pane")
                        ) {
                            VideoPaneContent(
                                bitmap = targetBitmap,
                                label = "YOUR FOOTAGE",
                                badgeColor = StudioCyan,
                                metadata = targetMetadata,
                                currentFrame = targetFrameCurrent,
                                totalFrames = targetFrameTotal,
                                currentTimeMs = currentTimeMs,
                                totalDurationMs = targetDuration,
                                subject = if (showSubjectGuide) targetSubject else null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                SplitLayoutMode.STACKED -> {
                    // Vertical Stacked: Reference top, Target bottom
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Top Pane: Reference
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color(0xFF0F172A))
                        ) {
                            VideoPaneContent(
                                bitmap = referenceBitmap,
                                label = "REFERENCE MOTION",
                                badgeColor = StudioAmber,
                                metadata = referenceMetadata,
                                currentFrame = refFrameCurrent,
                                totalFrames = refFrameTotal,
                                currentTimeMs = mappedRefTime,
                                totalDurationMs = refDuration,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Divider Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(StudioCyan.copy(alpha = 0.6f))
                        )

                        // Bottom Pane: User Footage
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .background(Color(0xFF0F172A))
                        ) {
                            VideoPaneContent(
                                bitmap = targetBitmap,
                                label = "YOUR FOOTAGE",
                                badgeColor = StudioCyan,
                                metadata = targetMetadata,
                                currentFrame = targetFrameCurrent,
                                totalFrames = targetFrameTotal,
                                currentTimeMs = currentTimeMs,
                                totalDurationMs = targetDuration,
                                subject = if (showSubjectGuide) targetSubject else null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                SplitLayoutMode.WIPE_SLIDER -> {
                    // Interactive wipe slider overlaying both videos
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    ) {
                        val boxWidth = maxWidth
                        val boxHeight = maxHeight

                        // Bottom Layer: User's Footage
                        if (targetBitmap != null) {
                            Image(
                                bitmap = targetBitmap.asImageBitmap(),
                                contentDescription = "User's Footage",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            PlaceholderContent(label = "User's Footage Available")
                        }

                        // Target Label Pill (Right)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioCyan.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "YOUR FOOTAGE • Frame $targetFrameCurrent",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Top Layer: Reference Video clipped to wipeFraction width
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(wipeFraction)
                                .align(Alignment.CenterStart)
                        ) {
                            if (referenceBitmap != null) {
                                Image(
                                    bitmap = referenceBitmap.asImageBitmap(),
                                    contentDescription = "Reference Video",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                PlaceholderContent(label = "Reference Video")
                            }

                            // Reference Label Pill (Left)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StudioAmber.copy(alpha = 0.90f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "REFERENCE • Frame $refFrameCurrent",
                                    color = Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Interactive Draggable Divider
                        val dividerX = (boxWidth * wipeFraction)
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(dividerX.roundToPx() - 12, 0) }
                                .width(24.dp)
                                .fillMaxHeight()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newFraction = (wipeFraction + dragAmount.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                                        wipeFraction = newFraction
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Divider bar
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(StudioCyan)
                            )

                            // Drag Handle Icon
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(StudioCyan)
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SyncAlt,
                                    contentDescription = "Drag Divider",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // High Precision Timecode & Frame Readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioSurfaceHighlight)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Target Time & Frame
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "TARGET",
                            color = StudioCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val curSec = currentTimeMs / 1000f
                    val totSec = targetDuration / 1000f
                    Text(
                        text = String.format("%02d:%05.2fs / %02d:%05.2fs", (curSec / 60).toInt(), curSec % 60, (totSec / 60).toInt(), totSec % 60),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = StudioTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Frame $targetFrameCurrent/$targetFrameTotal",
                        color = StudioTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Reference Synced Time & Frame
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StudioAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "REF",
                            color = StudioAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val refCurSec = mappedRefTime / 1000f
                    Text(
                        text = String.format("%02d:%05.2fs", (refCurSec / 60).toInt(), refCurSec % 60),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = StudioTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Frame $refFrameCurrent",
                        color = StudioTextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // High Precision Scrubber Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = if (targetDuration > 0) (currentTimeMs.toFloat() / targetDuration.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { frac ->
                        val targetMs = (frac * targetDuration).toLong()
                        onSeek(targetMs)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("split_scrubber_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyan,
                        activeTrackColor = StudioCyan,
                        inactiveTrackColor = StudioSurfaceHighlight
                    )
                )
            }

            // Playback Options Bar: Loop Control & Slow-Motion Speed Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Loop Mode Control Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLooping) StudioEmerald.copy(alpha = 0.15f) else StudioSurfaceHighlight)
                        .border(
                            1.dp,
                            if (isLooping) StudioEmerald.copy(alpha = 0.5f) else StudioBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onToggleLoop() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("playback_loop_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isLooping) Icons.Default.RepeatOn else Icons.Default.Repeat,
                        contentDescription = "Loop Playback",
                        tint = if (isLooping) StudioEmerald else StudioTextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isLooping) "LOOP ON" else "LOOP OFF",
                        color = if (isLooping) StudioEmerald else StudioTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Slow-Motion Speed Selector (0.25x, 0.5x, 1.0x, 2.0x) for micro frame analysis
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceHighlight)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.25f, 0.5f, 1.0f, 2.0f).forEach { speed ->
                        val isSelected = (playbackSpeed == speed)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) StudioCyan else Color.Transparent)
                                .clickable { onSpeedChange(speed) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                .testTag("playback_speed_${speed}x"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${speed}x",
                                color = if (isSelected) Color.Black else StudioTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Custom Playback Transport Controls: Frame-by-Frame, Seek Jumps & Play/Pause
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump to Start (0ms)
                IconButton(
                    onClick = { onSeek(0L) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceHighlight)
                        .testTag("playback_jump_start")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Jump to Start",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Step Backward 1 Frame (-33ms)
                IconButton(
                    onClick = onStepFrameBackward,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceHighlight)
                        .testTag("playback_step_backward_frame")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Step Backward 1 Frame",
                        tint = StudioCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Jump -1s
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioSurfaceHighlight)
                        .clickable { onJumpSeconds(-1.0f) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("playback_jump_back_1s"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "-1s", color = StudioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Primary Play / Pause Button
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StudioCyan)
                        .testTag("playback_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Jump +1s
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioSurfaceHighlight)
                        .clickable { onJumpSeconds(1.0f) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("playback_jump_forward_1s"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+1s", color = StudioTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Step Forward 1 Frame (+33ms)
                IconButton(
                    onClick = onStepFrameForward,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceHighlight)
                        .testTag("playback_step_forward_frame")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Step Forward 1 Frame",
                        tint = StudioCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Jump to End
                IconButton(
                    onClick = { onSeek(targetDuration) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceHighlight)
                        .testTag("playback_jump_end")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Jump to End",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Primary AI Analysis CTA
            StudioPrimaryButton(
                text = if (isAnalyzing) "ANALYZING MOTION ($((analysisProgress * 100).toInt())%)..." else "PROCESS AI MOTION TRANSFER",
                icon = Icons.Default.AutoAwesome,
                onClick = onStartAnalysis,
                enabled = !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("split_start_ai_analysis_button")
            )
        }
    }
}

@Composable
private fun VideoPaneContent(
    bitmap: Bitmap?,
    label: String,
    badgeColor: Color,
    metadata: VideoMetadata?,
    currentFrame: Int,
    totalFrames: Int,
    currentTimeMs: Long,
    totalDurationMs: Long,
    modifier: Modifier = Modifier,
    subject: SubjectRegion? = null
) {
    Box(modifier = modifier) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PlaceholderContent(label = label)
        }

        // Subject Guide Crosshair / Bounding Box (for User's Footage)
        if (subject != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxLeft = (subject.centerX - subject.width / 2f) * size.width
                val boxTop = (subject.centerY - subject.height / 2f) * size.height
                val boxW = subject.width * size.width
                val boxH = subject.height * size.height

                drawRect(
                    color = Color(0xFF10B981).copy(alpha = 0.85f),
                    topLeft = Offset(boxLeft, boxTop),
                    size = Size(boxW, boxH),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Crosshairs at center
                val cx = subject.centerX * size.width
                val cy = subject.centerY * size.height
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(cx - 10f, cy),
                    end = Offset(cx + 10f, cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(cx, cy - 10f),
                    end = Offset(cx, cy + 10f),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        // Top Header Overlay: Badge & Live Frame Counter
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(badgeColor.copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = label,
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "F $currentFrame/$totalFrames",
                    color = StudioCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Bottom Footer Overlay: Duration, FPS, and Live Millisecond Stamp
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (metadata != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${metadata.width}x${metadata.height} • ${metadata.fps.toInt()}fps",
                        color = StudioTextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            val sec = currentTimeMs / 1000f
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = String.format("%02d:%05.2fs", (sec / 60).toInt(), sec % 60),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun PlaceholderContent(label: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.35f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
    }
}

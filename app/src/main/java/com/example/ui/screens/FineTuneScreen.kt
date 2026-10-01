package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimingMappingMode
import com.example.ui.components.CinematicCard
import com.example.ui.components.KeyframeTimelineView
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioStage
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun FineTuneScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val keyframes by viewModel.targetKeyframes.collectAsState()
    val motionIntensity by viewModel.motionIntensity.collectAsState()
    val timingMode by viewModel.timingMode.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val targetMeta by viewModel.targetMetadata.collectAsState()
    val totalDur = targetMeta?.durationMs ?: 10000L

    val isAutoSaving by viewModel.isAutoSaving.collectAsState()
    val lastSavedTime by viewModel.lastSavedTime.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stage Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "5", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text(
                        text = "Fine-Tune Motion",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Adjust motion intensity, timing curves, and custom keyframes",
                        fontSize = 12.sp,
                        color = StudioTextSecondary
                    )
                }
            }

            if (isAutoSaving) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(StudioCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "AUTO-SAVING...",
                        color = StudioCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (lastSavedTime != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SAVED TO ROOM",
                        color = Color(0xFF059669),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Global Motion Intensity Slider
        CinematicCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Motion Intensity Multiplier",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                    }
                    Text(
                        text = String.format("%.2fx", motionIntensity),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioCyan
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Scales the amplitude of all zoom-ins, whip pans, and tilt rotations proportionally.",
                    fontSize = 11.sp,
                    color = StudioTextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = motionIntensity,
                    onValueChange = { viewModel.setMotionIntensity(it) },
                    valueRange = 0.25f..2.50f,
                    colors = SliderDefaults.colors(
                        thumbColor = StudioCyan,
                        activeTrackColor = StudioCyan,
                        inactiveTrackColor = Color(0xFF1E2838)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("motion_intensity_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Subtle (0.25x)", fontSize = 10.sp, color = StudioTextSecondary)
                    Text(text = "Default (1.0x)", fontSize = 10.sp, color = StudioTextSecondary)
                    Text(text = "Exaggerated (2.5x)", fontSize = 10.sp, color = StudioTextSecondary)
                }
            }
        }

        // Duration & Timing Mapping Mode
        CinematicCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Timeline Mapping Strategy",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TimingMappingMode.values().forEach { mode ->
                    val isSelected = mode == timingMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) StudioSurfaceHighlight else Color.Transparent)
                            .clickable { viewModel.setTimingMode(mode) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) StudioCyan else Color(0xFF263248)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.Black)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = mode.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) StudioTextPrimary else StudioTextSecondary
                            )
                            Text(
                                text = mode.subtitle,
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Interactive Keyframe Track
        KeyframeTimelineView(
            keyframes = keyframes,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDur,
            onSeek = { viewModel.seekTo(it) },
            onAddKeyframe = { viewModel.addKeyframe(it) },
            onUpdateKeyframe = { viewModel.updateKeyframe(it) },
            onDeleteKeyframe = { viewModel.deleteKeyframe(it) },
            modifier = Modifier.fillMaxWidth()
        )

        // Reset Keyframes & Apply AI Again Buttons
        CinematicCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "AI State Management",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioSecondaryButton(
                        text = "RESET KEYFRAMES",
                        icon = Icons.Default.RestartAlt,
                        onClick = { viewModel.resetKeyframes() },
                        modifier = Modifier.weight(1f),
                        testTag = "reset_keyframes_button"
                    )
                    StudioPrimaryButton(
                        text = "APPLY AI AGAIN",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { viewModel.runAiAnalysis() },
                        modifier = Modifier.weight(1f),
                        testTag = "apply_ai_again_button"
                    )
                }
            }
        }

        // Proceed to Export
        StudioPrimaryButton(
            text = "Proceed to Render & Export",
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = { viewModel.setStage(StudioStage.RENDER_EXPORT) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "proceed_to_export_button"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

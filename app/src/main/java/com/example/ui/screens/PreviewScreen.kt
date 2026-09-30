package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Tune
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
import com.example.ui.components.KeyframeTimelineView
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioStage
import com.example.ui.components.VideoPreviewBox
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun PreviewScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val targetMeta by viewModel.targetMetadata.collectAsState()
    val targetSubject by viewModel.targetSubject.collectAsState()
    val frameBitmap by viewModel.previewFrameBitmap.collectAsState()
    val keyframes by viewModel.targetKeyframes.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val scrollState = rememberScrollState()

    val totalDur = targetMeta?.durationMs ?: 10000L
    val aspect = targetMeta?.aspectRatio ?: (16f / 9f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stage Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                    Text(text = "4", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text(
                        text = "Real-Time Preview",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "Original vs Transformed with Keyframes",
                        fontSize = 12.sp,
                        color = StudioTextSecondary
                    )
                }
            }
        }

        // Live Video Viewport with Transform & Split Mode
        VideoPreviewBox(
            frameBitmap = frameBitmap,
            currentTimeMs = currentTimeMs,
            totalDurationMs = totalDur,
            keyframes = keyframes,
            targetSubject = targetSubject,
            isPlaying = isPlaying,
            onTogglePlay = { viewModel.togglePlayPause() },
            onSeek = { viewModel.seekTo(it) },
            aspectRatio = aspect,
            modifier = Modifier.fillMaxWidth()
        )

        // Interactive Keyframe Timeline
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

        // Bottom CTA Buttons: Fine Tune & Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StudioSecondaryButton(
                text = "Fine-Tune Motion",
                icon = Icons.Default.Tune,
                onClick = { viewModel.setStage(StudioStage.FINE_TUNE) },
                modifier = Modifier.weight(1f),
                testTag = "preview_fine_tune_button"
            )
            StudioPrimaryButton(
                text = "Render & Export",
                icon = Icons.Default.FileDownload,
                onClick = { viewModel.setStage(StudioStage.RENDER_EXPORT) },
                modifier = Modifier.weight(1f),
                testTag = "preview_export_button"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

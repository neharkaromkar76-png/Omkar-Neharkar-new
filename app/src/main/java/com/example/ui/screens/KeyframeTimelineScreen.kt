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
import androidx.compose.material.icons.filled.Visibility
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
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun KeyframeTimelineScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val keyframes by viewModel.targetKeyframes.collectAsState()
    val currentTimeMs by viewModel.currentTimeMs.collectAsState()
    val targetMeta by viewModel.targetMetadata.collectAsState()
    val totalDur = targetMeta?.durationMs ?: 10000L
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
                    text = "Keyframe Timeline",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Inspect and manipulate motion nodes and curves",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )
            }
        }

        // Timeline Component
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

        // Proceed to Preview Button
        StudioPrimaryButton(
            text = "Preview Transformed Video",
            icon = Icons.Default.Visibility,
            onClick = { viewModel.setStage(StudioStage.PREVIEW) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "timeline_to_preview_button"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

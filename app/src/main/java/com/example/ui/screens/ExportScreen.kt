package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.components.CinematicCard
import com.example.ui.components.MetricChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun ExportScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exportConfig by viewModel.exportConfig.collectAsState()
    val isRendering by viewModel.isRendering.collectAsState()
    val renderMessage by viewModel.renderStageMessage.collectAsState()
    val renderProgress by viewModel.renderProgress.collectAsState()
    val exportProgressPercentage by viewModel.exportProgressFlow.collectAsState(initial = (renderProgress * 100).toInt())
    val renderedFile by viewModel.renderedFile.collectAsState()
    val targetMeta by viewModel.targetMetadata.collectAsState()

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
                Text(text = "6", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Column {
                Text(
                    text = "Render & Export",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Hardware H.264 encoding with relative motion transformations",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )
            }
        }

        if (isRendering) {
            // Render in Progress Card
            CinematicCard(
                borderColor = StudioCyan,
                backgroundColor = Color(0xFF0C1320)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    val displayPercent = exportProgressPercentage.coerceIn(0, 100)
                    CircularProgressIndicator(
                        progress = { displayPercent / 100f },
                        color = StudioCyan,
                        trackColor = StudioSurfaceHighlight,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(60.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = renderMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$displayPercent% Rendered",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = StudioCyan,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { displayPercent / 100f },
                        color = StudioCyan,
                        trackColor = Color(0xFF1E2838),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    StudioSecondaryButton(
                        text = "Cancel Rendering",
                        icon = Icons.Default.Cancel,
                        onClick = { viewModel.cancelRendering() },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        testTag = "cancel_render_button"
                    )
                }
            }
        } else if (renderedFile != null && renderedFile!!.exists()) {
            // Success Card with Video File Info
            CinematicCard(borderColor = StudioEmerald.copy(alpha = 0.6f)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StudioEmerald, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Export Completed!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(StudioEmerald.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "REAL MP4",
                                color = StudioEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = renderedFile!!.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )

                    val fileSizeMb = String.format("%.2f MB", renderedFile!!.length() / (1024f * 1024f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricChip(label = "File Size", value = fileSizeMb, highlight = true)
                        MetricChip(label = "Format", value = "H.264 / AVC")
                        MetricChip(label = "FPS", value = "${exportConfig.fps} fps")
                        MetricChip(label = "Resolution", value = "${exportConfig.width}x${exportConfig.height}")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Share / Open Video Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioPrimaryButton(
                            text = "SHARE MP4 VIDEO",
                            icon = Icons.Default.Share,
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        renderedFile!!
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "video/mp4"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Edited Video"))
                                } catch (_: Exception) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Exported Video at: ${renderedFile!!.absolutePath}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Video Exported"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "share_exported_video_button"
                        )

                        StudioSecondaryButton(
                            text = "Re-Render",
                            icon = Icons.Default.FileDownload,
                            onClick = { viewModel.renderAndExport() },
                            modifier = Modifier.weight(0.7f),
                            testTag = "rerender_button"
                        )
                    }
                }
            }
        } else {
            // Configuration Card
            CinematicCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Export Settings",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )

                    // Resolution Options
                    Column {
                        Text(text = "OUTPUT RESOLUTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val resolutions = listOf(
                            Triple("1080p (Full HD)", 1080, 1920),
                            Triple("720p (HD Fast)", 720, 1280),
                            Triple("Square 1:1", 1080, 1080)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            resolutions.forEach { (name, w, h) ->
                                val selected = exportConfig.width == w && exportConfig.height == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) StudioCyan else StudioSurfaceHighlight)
                                        .clickable {
                                            viewModel.updateExportConfig(
                                                exportConfig.copy(resolutionName = name, width = w, height = h)
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.Black else StudioTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // FPS Options
                    Column {
                        Text(text = "FRAME RATE (FPS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(24, 30, 60).forEach { fpsVal ->
                                val selected = exportConfig.fps == fpsVal
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) StudioAmber else StudioSurfaceHighlight)
                                        .clickable {
                                            viewModel.updateExportConfig(exportConfig.copy(fps = fpsVal))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$fpsVal FPS",
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.Black else StudioTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Quality Presets
                    Column {
                        Text(text = "QUALITY PRESET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Fast", "Balanced", "High Quality").forEach { preset ->
                                val selected = exportConfig.qualityPreset == preset
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) StudioCyan else StudioSurfaceHighlight)
                                        .clickable {
                                            viewModel.updateExportConfig(exportConfig.copy(qualityPreset = preset))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.Black else StudioTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Audio Option
                    Column {
                        Text(text = "AUDIO TRACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isKeep = exportConfig.audioMode == "Keep Target Audio"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isKeep) StudioSurfaceHighlight else Color.Transparent)
                                    .clickable {
                                        viewModel.updateExportConfig(exportConfig.copy(audioMode = "Keep Target Audio"))
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Keep Audio", fontSize = 12.sp, color = StudioTextPrimary)
                            }

                            val isMute = exportConfig.audioMode == "Mute Target Audio"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isMute) StudioSurfaceHighlight else Color.Transparent)
                                    .clickable {
                                        viewModel.updateExportConfig(exportConfig.copy(audioMode = "Mute Target Audio"))
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.VolumeMute, contentDescription = null, tint = StudioAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Mute Audio", fontSize = 12.sp, color = StudioTextPrimary)
                            }
                        }
                    }
                }
            }

            // Render CTA Button
            StudioPrimaryButton(
                text = "RENDER & EXPORT VIDEO",
                icon = Icons.Default.FileDownload,
                onClick = { viewModel.renderAndExport() },
                modifier = Modifier.fillMaxWidth(),
                testTag = "start_render_button"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

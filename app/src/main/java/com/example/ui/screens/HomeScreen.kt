package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VideoMetadata
import com.example.ui.components.CinematicCard
import com.example.ui.components.MetricChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioStage
import com.example.ui.components.StudioStatusPill
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioPrimary
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.theme.StudioTextTertiary
import com.example.ui.viewmodel.StudioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: StudioViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.allProjects.collectAsState()
    val referenceMetadata by viewModel.referenceMetadata.collectAsState()
    val referenceThumbnail by viewModel.referenceThumbnail.collectAsState()
    val targetMetadata by viewModel.targetMetadata.collectAsState()
    val targetThumbnail by viewModel.targetThumbnail.collectAsState()

    val bothSelected = referenceMetadata != null && targetMetadata != null

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header: App Name / Logo
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AI Motion Studio",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioTextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            StudioStatusPill(text = "PRO", color = StudioCyan)
                        }
                        Text(
                            text = "Precision Camera Motion Transfer",
                            fontSize = 12.sp,
                            color = StudioTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.testTag("home_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Card 1: Reference Motion Card (Part 2)
        item {
            CinematicCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioCyan.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = StudioCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reference Motion",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextPrimary
                                )
                                Text(
                                    text = "Motion Source",
                                    fontSize = 11.sp,
                                    color = StudioCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (referenceMetadata != null) {
                            StudioStatusPill(text = "READY", color = StudioEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose the video whose camera movement you want to reproduce.",
                        fontSize = 13.sp,
                        color = StudioTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (referenceMetadata == null) {
                        StudioPrimaryButton(
                            text = "Select Reference Video",
                            icon = Icons.Default.Videocam,
                            onClick = { viewModel.setStage(StudioStage.REFERENCE) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "select_reference_video_button"
                        )
                    } else {
                        // Display Reference Preview Card
                        VideoInfoPreviewCard(
                            metadata = referenceMetadata!!,
                            thumbnail = referenceThumbnail,
                            badgeLabel = "REFERENCE MOTION SOURCE",
                            badgeColor = StudioCyan,
                            onChangeClick = { viewModel.setStage(StudioStage.REFERENCE) },
                            testTag = "change_reference_button"
                        )
                    }
                }
            }
        }

        // Card 2: Target Video Card (Part 2)
        item {
            CinematicCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioAmber.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = StudioAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Target Video",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextPrimary
                                )
                                Text(
                                    text = "Receiving Footage",
                                    fontSize = 11.sp,
                                    color = StudioAmber,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (targetMetadata != null) {
                            StudioStatusPill(text = "READY", color = StudioEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Choose the video that will receive the reference motion.",
                        fontSize = 13.sp,
                        color = StudioTextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (targetMetadata == null) {
                        StudioPrimaryButton(
                            text = "Select Target Video",
                            icon = Icons.Default.Movie,
                            onClick = { viewModel.setStage(StudioStage.TARGET) },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "select_target_video_button"
                        )
                    } else {
                        // Display Target Preview Card
                        VideoInfoPreviewCard(
                            metadata = targetMetadata!!,
                            thumbnail = targetThumbnail,
                            badgeLabel = "TARGET FOOTAGE (PRESERVED)",
                            badgeColor = StudioAmber,
                            onChangeClick = { viewModel.setStage(StudioStage.TARGET) },
                            testTag = "change_target_button"
                        )
                    }
                }
            }
        }

        // Action: Proceed to AI Analyze when both selected
        if (bothSelected) {
            item {
                CinematicCard(
                    borderColor = StudioEmerald.copy(alpha = 0.5f),
                    backgroundColor = Color(0xFFF0FDF4)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StudioEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Motion Transfer Pair Configured",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = StudioTextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ready to extract reference camera curves and apply to target.",
                            fontSize = 12.sp,
                            color = StudioTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        StudioPrimaryButton(
                            text = "AI ANALYZE & TRANSFER MOTION",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                viewModel.setStage(StudioStage.ANALYSIS)
                                viewModel.runAiAnalysis()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "start_motion_transfer_button"
                        )
                    }
                }
            }
        }

        // Recent Projects Section
        if (projects.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Projects",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Text(
                        text = "${projects.size} saved",
                        fontSize = 12.sp,
                        color = StudioTextTertiary
                    )
                }
            }

            items(projects, key = { it.id }) { project ->
                CinematicCard(modifier = Modifier.clickable { viewModel.loadProject(project) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = project.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = StudioTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val dateStr = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(project.updatedAt))
                            Text(
                                text = "$dateStr · ${project.timingMode}",
                                fontSize = 11.sp,
                                color = StudioTextSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.deleteProject(project.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = StudioRed.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Open",
                                tint = StudioTextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VideoInfoPreviewCard(
    metadata: VideoMetadata,
    thumbnail: Bitmap?,
    badgeLabel: String,
    badgeColor: Color,
    onChangeClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata.fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = StudioTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${metadata.formattedDuration} · ${metadata.width}x${metadata.height}",
                    fontSize = 11.sp,
                    color = StudioTextSecondary
                )
            }

            StudioSecondaryButton(
                text = "Change",
                onClick = onChangeClick,
                testTag = testTag
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics: duration, resolution, FPS, file size (Part 2 requirement)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricChip(label = "Duration", value = metadata.formattedDuration)
            MetricChip(label = "Resolution", value = "${metadata.width}x${metadata.height}")
            MetricChip(label = "FPS", value = "${metadata.fps.toInt()}")
            val sizeMb = if (metadata.fileSize > 0) String.format("%.1f MB", metadata.fileSize / (1024f * 1024f)) else "~12 MB"
            MetricChip(label = "File Size", value = sizeMb)
        }
    }
}

package com.example.ui.screens

import android.net.Uri
import com.example.engine.analysis.SampleMediaHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
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
import com.example.ui.components.CinematicCard
import com.example.ui.components.MetricChip
import com.example.ui.components.StudioPrimaryButton
import com.example.ui.components.StudioSecondaryButton
import com.example.ui.components.StudioStage
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceHighlight
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.ui.viewmodel.StudioViewModel

@Composable
fun TargetVideoScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val refMeta by viewModel.referenceMetadata.collectAsState()
    val targetMeta by viewModel.targetMetadata.collectAsState()
    val targetThumb by viewModel.targetThumbnail.collectAsState()
    val targetSubject by viewModel.targetSubject.collectAsState()
    val scrollState = rememberScrollState()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectTargetVideo(uri)
        }
    }

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
                Text(text = "2", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Column {
                Text(
                    text = "Select Target Video",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Your original footage that will receive the transferred motion",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )
            }
        }

        // Selection / Preview Box
        CinematicCard(
            borderColor = if (targetMeta != null) StudioCyan.copy(alpha = 0.5f) else StudioBorder
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (targetThumb != null && targetMeta != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = targetThumb!!.asImageBitmap(),
                            contentDescription = "Target Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = targetMeta!!.formattedDuration,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = targetMeta!!.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StudioTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricChip(label = "Duration", value = targetMeta!!.formattedDuration, icon = Icons.Default.Timer)
                        MetricChip(label = "Resolution", value = "${targetMeta!!.width}x${targetMeta!!.height}")
                        MetricChip(label = "FPS", value = "${targetMeta!!.fps.toInt()} fps")
                        MetricChip(label = "Aspect", value = targetMeta!!.aspectRatioString)
                    }

                    // Subject Detection result chip
                    if (targetSubject != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StudioAmber.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = StudioAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Subject Focus: ${targetSubject!!.label} (${(targetSubject!!.confidence * 100).toInt()}% conf)",
                                fontSize = 12.sp,
                                color = StudioAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioSecondaryButton(
                            text = "Change Video",
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "change_target_button"
                        )
                        StudioPrimaryButton(
                            text = "Analyze & Transfer",
                            icon = Icons.Default.AutoAwesome,
                            onClick = {
                                viewModel.setStage(StudioStage.ANALYSIS)
                                viewModel.runAiAnalysis()
                            },
                            modifier = Modifier.weight(1.3f),
                            testTag = "analyze_motion_button"
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = StudioCyan,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Your Target Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The video you recorded that will be dynamically reframed, zoomed, and tracked according to the reference pattern.",
                        fontSize = 12.sp,
                        color = StudioTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    StudioPrimaryButton(
                        text = "SELECT VIDEO FROM DEVICE",
                        icon = Icons.Default.FileOpen,
                        onClick = {
                            pickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "select_target_picker_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StudioSecondaryButton(
                        text = "Use Built-in Target Gameplay / Vlog Sample",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            val sampleUri = Uri.parse(SampleMediaHelper.TARGET_SAMPLE_URI)
                            viewModel.selectTargetVideo(sampleUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sample_target_button"
                    )
                }
            }
        }

        // Duration Comparison / Compatibility Card
        if (refMeta != null && targetMeta != null) {
            CinematicCard {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timeline & Duration Adaptation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StudioTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Reference Duration:", fontSize = 12.sp, color = StudioTextSecondary)
                        Text(text = refMeta!!.formattedDuration, fontSize = 12.sp, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Target Duration:", fontSize = 12.sp, color = StudioTextSecondary)
                        Text(text = targetMeta!!.formattedDuration, fontSize = 12.sp, color = StudioTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Automatic Strategy: Normalized (0-100%) mapping will adapt all reference zooms and motion points proportionally across the target duration.",
                        fontSize = 11.sp,
                        color = StudioCyan,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

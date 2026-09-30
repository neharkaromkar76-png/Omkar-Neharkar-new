package com.example.ui.screens

import android.net.Uri
import com.example.engine.analysis.SampleMediaHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MotionPhotosOn
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
import androidx.compose.ui.platform.LocalContext
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
import java.io.File
import java.io.FileOutputStream

@Composable
fun ReferenceVideoScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val refMeta by viewModel.referenceMetadata.collectAsState()
    val refThumb by viewModel.referenceThumbnail.collectAsState()
    val scrollState = rememberScrollState()

    // Android Photo Picker (zero runtime permissions, Google Play compliant)
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.selectReferenceVideo(uri)
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
                Text(text = "1", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Column {
                Text(
                    text = "Select Reference Video",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Source video showing how you want the camera motion to look",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )
            }
        }

        // Upload / Selection Box
        CinematicCard(
            borderColor = if (refMeta != null) StudioCyan.copy(alpha = 0.5f) else StudioBorder
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (refThumb != null && refMeta != null) {
                    // Thumbnail preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = refThumb!!.asImageBitmap(),
                            contentDescription = "Reference Thumbnail",
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
                                text = refMeta!!.formattedDuration,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = refMeta!!.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StudioTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metadata Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricChip(label = "Duration", value = refMeta!!.formattedDuration, icon = Icons.Default.Timer)
                        MetricChip(label = "Resolution", value = "${refMeta!!.width}x${refMeta!!.height}")
                        MetricChip(label = "FPS", value = "${refMeta!!.fps.toInt()} fps")
                        MetricChip(label = "Aspect", value = refMeta!!.aspectRatioString)
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
                            testTag = "change_reference_button"
                        )
                        StudioPrimaryButton(
                            text = "Next: Target Video",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = { viewModel.setStage(StudioStage.TARGET) },
                            modifier = Modifier.weight(1.2f),
                            testTag = "proceed_to_target_button"
                        )
                    }
                } else {
                    // Empty state / prompt
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = StudioCyan,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Choose Reference Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select any edit, gameplay clip, TikTok reel, or drone footage with camera motion you want to replicate.",
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
                        testTag = "select_reference_picker_button"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Demo / Built-in Sample Loader
                    StudioSecondaryButton(
                        text = "Use Built-in Kinetic Reference Sample",
                        icon = Icons.Default.AutoAwesome,
                        onClick = {
                            val sampleUri = Uri.parse(SampleMediaHelper.REFERENCE_SAMPLE_URI)
                            viewModel.selectReferenceVideo(sampleUri)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "sample_reference_button"
                    )
                }
            }
        }

        // Feature explanation card
        CinematicCard {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "What AI Keyframe Studio Extracts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = StudioTextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val features = listOf(
                    "Relative zoom-in and punch-out curves",
                    "Horizontal whip pans & tracking pan vectors",
                    "Tilt dynamics and camera roll/rotation",
                    "Speed ramp intervals and acceleration",
                    "Cubic & Bézier transition easing between moments"
                )
                features.forEach { feat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(StudioAmber)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = feat, fontSize = 12.sp, color = StudioTextSecondary)
                    }
                }
            }
        }
    }
}

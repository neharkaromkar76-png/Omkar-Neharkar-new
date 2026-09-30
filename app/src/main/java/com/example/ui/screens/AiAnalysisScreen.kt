package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MotionSample
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

@Composable
fun AiAnalysisScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val stageMessage by viewModel.analysisStageMessage.collectAsState()
    val progress by viewModel.analysisProgress.collectAsState()
    val aiResult by viewModel.aiResult.collectAsState()
    val targetKeyframes by viewModel.targetKeyframes.collectAsState()
    val referenceMetadata by viewModel.referenceMetadata.collectAsState()
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column {
                Text(
                    text = "AI Motion Analysis",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary
                )
                Text(
                    text = "Gemini Video Understanding + Computer Vision Optical Flow",
                    fontSize = 12.sp,
                    color = StudioTextSecondary
                )
            }
        }

        // Processing Card (Real 0% -> 100% Progress)
        if (isAnalyzing) {
            CinematicCard(
                borderColor = StudioCyan.copy(alpha = 0.5f),
                backgroundColor = StudioSurface
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            color = StudioCyan,
                            trackColor = StudioSurfaceHighlight,
                            modifier = Modifier.size(72.dp),
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = stageMessage,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StudioTextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Analyzing frame-to-frame background kinematics...",
                        fontSize = 12.sp,
                        color = StudioTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        color = StudioCyan,
                        trackColor = StudioSurfaceHighlight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    StudioSecondaryButton(
                        text = "Cancel Analysis",
                        icon = Icons.Default.Cancel,
                        onClick = { viewModel.cancelAnalysis() },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        testTag = "cancel_analysis_button"
                    )
                }
            }
        } else if (aiResult != null && !aiResult!!.isSuccess) {
            // Failure Card (Part 20)
            CinematicCard(
                borderColor = StudioRed.copy(alpha = 0.5f),
                backgroundColor = Color(0xFFFEF2F2)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Analysis Error",
                        tint = StudioRed,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Motion Analysis Notice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = aiResult!!.errorMessage ?: "Camera motion could not be reliably detected.",
                        fontSize = 13.sp,
                        color = StudioTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    StudioPrimaryButton(
                        text = "Retry Motion Analysis",
                        icon = Icons.Default.Refresh,
                        onClick = { viewModel.runAiAnalysis(forceReanalyze = true) },
                        modifier = Modifier.fillMaxWidth(0.7f),
                        testTag = "retry_analysis_button"
                    )
                }
            }
        } else if (aiResult != null && aiResult!!.isSuccess) {
            val result = aiResult!!
            val timeline = result.motionTimeline
            val meta = referenceMetadata

            // Card 1: Motion Match Score & Status (Part 14)
            CinematicCard(
                borderColor = StudioEmerald.copy(alpha = 0.4f),
                backgroundColor = StudioSurface
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StudioEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Motion Analysis Complete",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = StudioTextPrimary
                            )
                        }

                        StudioStatusPill(text = "MOTION TRANSFER READY", color = StudioEmerald)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Camera Kinematics: ${result.motionStyle}",
                        fontSize = 14.sp,
                        color = StudioTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.notes,
                        fontSize = 12.sp,
                        color = StudioTextSecondary,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Complexity, Events, Movement, Confidence (Part 14)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val complexityStr = when {
                            result.events.size >= 4 -> "High"
                            result.events.size >= 2 -> "Moderate"
                            else -> "Subtle"
                        }
                        MetricChip(label = "Complexity", value = complexityStr)
                        MetricChip(label = "Events", value = "${result.events.size} detected")
                        val moveStr = if (result.avgMotion > 0.15f) "Dynamic" else "Smooth"
                        MetricChip(label = "Movement", value = moveStr)
                        MetricChip(label = "Confidence", value = "${(result.overallConfidence * 100).toInt()}%", icon = Icons.Default.Sensors, highlight = true)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Duration, FPS, Resolution, Frames Analyzed (Part 13)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricChip(label = "Duration", value = String.format("%.2fs", result.referenceDurationSec))
                        MetricChip(label = "FPS", value = "${result.detectedFps.toInt()}")
                        val resStr = if (meta != null) "${meta.width}x${meta.height}" else "1080p"
                        MetricChip(label = "Resolution", value = resStr)
                        MetricChip(label = "Samples", value = "${timeline?.samples?.size ?: 0}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 3: Kinematic Extrema
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricChip(label = "Max Zoom", value = String.format("%.2fx", result.maxZoom))
                        MetricChip(label = "Max X Pan", value = String.format("%.1f%%", result.maxX * 100f))
                        MetricChip(label = "Max Y Tilt", value = String.format("%.1f%%", result.maxY * 100f))
                        MetricChip(label = "Max Roll", value = String.format("%.1f°", result.maxRotation))
                    }
                }
            }

            // Card 2: Real Visual Motion Timeline Graph (Part 13)
            if (timeline != null && timeline.samples.isNotEmpty()) {
                CinematicCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = StudioCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Measured Reference Motion Curve",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioTextPrimary
                                )
                            }
                            StudioStatusPill(text = "REAL DATA", color = StudioCyan)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Curve Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CurveLegendItem(label = "Zoom", color = StudioCyan)
                            CurveLegendItem(label = "X Motion", color = StudioAmber)
                            CurveLegendItem(label = "Y Motion", color = StudioEmerald)
                            CurveLegendItem(label = "Rotation", color = Color(0xFF7C3AED))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        RealMotionCurveGraph(
                            samples = timeline.samples,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }

            // Card 3: Detected Motion Events breakdown (Part 13)
            if (result.events.isNotEmpty()) {
                CinematicCard {
                    Column {
                        Text(
                            text = "Detected Kinematic Events",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        result.events.forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StudioSurfaceElevated)
                                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val eventColor = when (event.type) {
                                        com.example.data.model.MotionType.WHIP_PAN -> StudioAmber
                                        com.example.data.model.MotionType.HOLD -> StudioEmerald
                                        com.example.data.model.MotionType.ZOOM_IN, com.example.data.model.MotionType.ZOOM_OUT -> StudioCyan
                                        else -> StudioPrimary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(eventColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = event.type.displayName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StudioTextPrimary
                                        )
                                        Text(
                                            text = event.description,
                                            fontSize = 11.sp,
                                            color = StudioTextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = String.format("%.2fs - %.2fs", event.startTimeMs / 1000f, event.endTimeMs / 1000f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = StudioTextPrimary
                                    )
                                    Text(
                                        text = "${(event.confidence * 100).toInt()}% conf",
                                        fontSize = 10.sp,
                                        color = StudioTextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Re-analyze or Proceed to Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioSecondaryButton(
                    text = "Re-Analyze",
                    icon = Icons.Default.Refresh,
                    onClick = { viewModel.runAiAnalysis(forceReanalyze = true) },
                    modifier = Modifier.weight(1f),
                    testTag = "reanalyze_button"
                )
                StudioPrimaryButton(
                    text = "View Motion Preview",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = { viewModel.setStage(StudioStage.PREVIEW) },
                    modifier = Modifier.weight(1.5f),
                    testTag = "proceed_to_preview_button"
                )
            }
        } else {
            // Idle State
            CinematicCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(StudioCyan.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Ready for AI Motion Analysis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extract optical flow, camera zoom curves, and tracking pans from reference video to apply to target.",
                        fontSize = 13.sp,
                        color = StudioTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    StudioPrimaryButton(
                        text = "ANALYZE REFERENCE MOTION",
                        icon = Icons.Default.AutoAwesome,
                        onClick = { viewModel.runAiAnalysis() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "start_analysis_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun CurveLegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = StudioTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Renders the real measured motion trajectory curves for Zoom, X Motion, Y Motion, and Rotation
 */
@Composable
private fun RealMotionCurveGraph(
    samples: List<MotionSample>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val midY = h / 2f

            // Baseline reference center grid line
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1f
            )

            if (samples.size < 2) return@Canvas

            val count = samples.size
            val maxZ = maxOf(1.2f, samples.maxOf { it.scale })
            val minZ = minOf(0.8f, samples.minOf { it.scale })
            val spanZ = maxOf(0.2f, maxZ - minZ)

            val maxX = maxOf(0.08f, samples.maxOf { kotlin.math.abs(it.translationX) })
            val maxY = maxOf(0.08f, samples.maxOf { kotlin.math.abs(it.translationY) })
            val maxR = maxOf(3.0f, samples.maxOf { kotlin.math.abs(it.rotationDegrees) })

            val zoomPath = Path()
            val xPath = Path()
            val yPath = Path()
            val rotPath = Path()

            for (i in 0 until count) {
                val s = samples[i]
                val px = (i.toFloat() / (count - 1).toFloat()) * w

                // Zoom: 0 at bottom, max at top
                val normZoom = (s.scale - minZ) / spanZ
                val pyZoom = h - (normZoom * (h * 0.85f) + h * 0.07f)

                // Translation X: centered at midY
                val normX = (s.translationX / maxX).coerceIn(-1f, 1f)
                val pyX = midY - normX * (h * 0.38f)

                // Translation Y: centered at midY
                val normY = (s.translationY / maxY).coerceIn(-1f, 1f)
                val pyY = midY - normY * (h * 0.38f)

                // Rotation: centered at midY
                val normR = (s.rotationDegrees / maxR).coerceIn(-1f, 1f)
                val pyR = midY - normR * (h * 0.38f)

                if (i == 0) {
                    zoomPath.moveTo(px, pyZoom)
                    xPath.moveTo(px, pyX)
                    yPath.moveTo(px, pyY)
                    rotPath.moveTo(px, pyR)
                } else {
                    zoomPath.lineTo(px, pyZoom)
                    xPath.lineTo(px, pyX)
                    yPath.lineTo(px, pyY)
                    rotPath.lineTo(px, pyR)
                }
            }

            // Draw paths with clean modern colors
            drawPath(path = xPath, color = Color(0xFFD97706), style = Stroke(width = 2.5f))
            drawPath(path = yPath, color = Color(0xFF059669), style = Stroke(width = 2.5f))
            drawPath(path = rotPath, color = Color(0xFF7C3AED), style = Stroke(width = 2.0f))
            drawPath(path = zoomPath, color = Color(0xFF2563EB), style = Stroke(width = 3.0f))
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Keyframe
import com.example.data.model.MotionSample
import com.example.data.model.MotionType
import com.example.engine.motion.Interpolator
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Visual overlay for the side-by-side video viewer that highlights tracked optical flow
 * motion vectors and keyframes extracted from the reference video.
 */
@Composable
fun MotionVectorAndKeyframeOverlay(
    currentTimeMs: Long,
    totalDurationMs: Long,
    keyframes: List<Keyframe>,
    modifier: Modifier = Modifier,
    motionSamples: List<MotionSample>? = null,
    showVectors: Boolean = true,
    showKeyframes: Boolean = true,
    showHud: Boolean = true,
    isReferencePane: Boolean = true,
    onSeekToKeyframe: ((Long) -> Unit)? = null
) {
    // Pulse animation for active keyframe anchor halo
    val infiniteTransition = rememberInfiniteTransition(label = "keyframe_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Evaluate continuous motion transform at current timestamp
    val currentTransform = remember(currentTimeMs, keyframes) {
        if (keyframes.isNotEmpty()) {
            Interpolator.evaluateKeyframeAtTime(currentTimeMs, keyframes)
        } else {
            val progress = (currentTimeMs.toFloat() / maxOf(1L, totalDurationMs).toFloat()).coerceIn(0f, 1f)
            val tSec = currentTimeMs / 1000f
            // Baseline sample trajectory
            when {
                tSec < 2.0f -> {
                    val p = tSec / 2.0f
                    com.example.engine.motion.KeyframeTransform(0.5f, 0.5f - 0.05f * p, 1.0f + 0.35f * p * p, 0f)
                }
                tSec < 3.5f -> {
                    val p = (tSec - 2.0f) / 1.5f
                    com.example.engine.motion.KeyframeTransform(0.5f - 0.15f * p, 0.45f, 1.35f, 0.5f * p)
                }
                tSec < 4.5f -> {
                    val p = (tSec - 3.5f) / 1.0f
                    com.example.engine.motion.KeyframeTransform(0.35f + 0.35f * p, 0.45f + 0.07f * p, 1.35f - 0.10f * p, -1.2f * p)
                }
                else -> {
                    com.example.engine.motion.KeyframeTransform(0.50f, 0.50f, 1.15f, 0f)
                }
            }
        }
    }

    // Evaluate delta motion for optical flow vector synthesis
    val deltaTransform = remember(currentTimeMs, keyframes) {
        val dt = 40L
        val tPrev = (currentTimeMs - dt).coerceAtLeast(0L)
        val tNext = (currentTimeMs + dt).coerceAtMost(maxOf(1L, totalDurationMs))
        val tf1 = if (keyframes.isNotEmpty()) Interpolator.evaluateKeyframeAtTime(tPrev, keyframes) else currentTransform
        val tf2 = if (keyframes.isNotEmpty()) Interpolator.evaluateKeyframeAtTime(tNext, keyframes) else currentTransform
        val spanSec = maxOf(0.001f, (tNext - tPrev) / 1000f)

        val velX = (tf2.x - tf1.x) / spanSec
        val velY = (tf2.y - tf1.y) / spanSec
        val deltaScale = (tf2.scale - tf1.scale) / spanSec
        val deltaRot = (tf2.rotation - tf1.rotation) / spanSec
        val speed = sqrt(velX * velX + velY * velY)

        MotionDelta(
            velX = velX,
            velY = velY,
            deltaScale = deltaScale,
            deltaRot = deltaRot,
            speed = speed,
            scale = currentTransform.scale,
            rotation = currentTransform.rotation
        )
    }

    // Check if current playhead is near an extracted keyframe anchor (within 220ms)
    val activeKeyframeIndex = remember(currentTimeMs, keyframes) {
        keyframes.indexOfFirst { kotlin.math.abs(it.timestampMs - currentTimeMs) <= 220L }
    }
    val activeKeyframe = if (activeKeyframeIndex >= 0) keyframes[activeKeyframeIndex] else null

    Box(modifier = modifier.fillMaxSize().testTag("motion_vector_and_keyframe_overlay")) {
        // Canvas Layer: Tracked Motion Vectors & Keyframe Framing Aperture
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Optical Flow Motion Vectors Grid
            if (showVectors) {
                val gridCols = 5
                val gridRows = 4
                val startX = canvasW * 0.15f
                val endX = canvasW * 0.85f
                val startY = canvasH * 0.18f
                val endY = canvasH * 0.82f

                val stepX = (endX - startX) / (gridCols - 1)
                val stepY = (endY - startY) / (gridRows - 1)

                val centerX = canvasW * 0.5f
                val centerY = canvasH * 0.5f

                for (r in 0 until gridRows) {
                    for (c in 0 until gridCols) {
                        val px = startX + c * stepX
                        val py = startY + r * stepY

                        // Translation component
                        val panVx = -deltaTransform.velX * canvasW * 0.25f
                        val panVy = -deltaTransform.velY * canvasH * 0.25f

                        // Zoom radial divergence component (outward if zoom in, inward if zoom out)
                        val radDx = (px - centerX) / (canvasW * 0.5f)
                        val radDy = (py - centerY) / (canvasH * 0.5f)
                        val zoomVx = radDx * deltaTransform.deltaScale * 18f
                        val zoomVy = radDy * deltaTransform.deltaScale * 18f

                        // Rotation tangential component
                        val rotAngleRad = Math.toRadians(deltaTransform.deltaRot.toDouble())
                        val rotVx = -radDy * deltaTransform.deltaRot * 2.2f
                        val rotVy = radDx * deltaTransform.deltaRot * 2.2f

                        // Combined optical flow displacement vector
                        val totalVx = panVx + zoomVx + rotVx
                        val totalVy = panVy + zoomVy + rotVy
                        val mag = sqrt(totalVx * totalVx + totalVy * totalVy)

                        // Clamp vector magnitude for pristine UI rendering
                        val clampedMag = mag.coerceIn(4f, 42f)
                        val angle = atan2(totalVy, totalVx)
                        val endVx = cos(angle) * clampedMag
                        val endVy = sin(angle) * clampedMag

                        // Color-code vector by velocity magnitude
                        val vectorColor = when {
                            mag > 28f -> Color(0xFFFF3B30) // Red/Orange for rapid whip pan
                            mag > 14f -> StudioAmber // Amber for moderate motion
                            else -> StudioCyan // Cyan for steady / subtle motion
                        }

                        // Origin tracked feature point (glow + core)
                        drawCircle(
                            color = vectorColor.copy(alpha = 0.25f),
                            radius = 6.dp.toPx(),
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = vectorColor,
                            radius = 3.dp.toPx(),
                            center = Offset(px, py)
                        )

                        // Directed Vector Arrow
                        if (clampedMag > 6f) {
                            val tipX = px + endVx
                            val tipY = py + endVy

                            drawLine(
                                color = vectorColor,
                                start = Offset(px, py),
                                end = Offset(tipX, tipY),
                                strokeWidth = if (mag > 28f) 2.5.dp.toPx() else 1.8.dp.toPx()
                            )

                            // Arrowhead wings
                            val arrowLen = 8.dp.toPx()
                            val arrowAngle = Math.toRadians(25.0)
                            val leftWingAngle = angle + Math.PI - arrowAngle
                            val rightWingAngle = angle + Math.PI + arrowAngle

                            val leftX = tipX + cos(leftWingAngle).toFloat() * arrowLen
                            val leftY = tipY + sin(leftWingAngle).toFloat() * arrowLen
                            val rightX = tipX + cos(rightWingAngle).toFloat() * arrowLen
                            val rightY = tipY + sin(rightWingAngle).toFloat() * arrowLen

                            drawLine(
                                color = vectorColor,
                                start = Offset(tipX, tipY),
                                end = Offset(leftX, leftY),
                                strokeWidth = 2.dp.toPx()
                            )
                            drawLine(
                                color = vectorColor,
                                start = Offset(tipX, tipY),
                                end = Offset(rightX, rightY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }
                    }
                }
            }

            // 2. Draw Extracted Keyframe Camera Framing Aperture Brackets
            if (showKeyframes && activeKeyframe != null) {
                val scale = activeKeyframe.scale.coerceIn(1.0f, 2.5f)
                val boxW = (canvasW / scale) * 0.90f
                val boxH = (canvasH / scale) * 0.90f
                val left = (canvasW - boxW) / 2f + (activeKeyframe.x - 0.5f) * canvasW * 0.2f
                val top = (canvasH - boxH) / 2f + (activeKeyframe.y - 0.5f) * canvasH * 0.2f

                val bracketLen = 22.dp.toPx()
                val bracketStroke = 2.5.dp.toPx()
                val bracketColor = StudioAmber.copy(alpha = pulseAlpha)

                // Top-Left corner bracket
                drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), bracketStroke)
                drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), bracketStroke)

                // Top-Right corner bracket
                drawLine(bracketColor, Offset(left + boxW, top), Offset(left + boxW - bracketLen, top), bracketStroke)
                drawLine(bracketColor, Offset(left + boxW, top), Offset(left + boxW, top + bracketLen), bracketStroke)

                // Bottom-Left corner bracket
                drawLine(bracketColor, Offset(left, top + boxH), Offset(left + bracketLen, top + boxH), bracketStroke)
                drawLine(bracketColor, Offset(left, top + boxH), Offset(left, top + boxH - bracketLen), bracketStroke)

                // Bottom-Right corner bracket
                drawLine(bracketColor, Offset(left + boxW, top + boxH), Offset(left + boxW - bracketLen, top + boxH), bracketStroke)
                drawLine(bracketColor, Offset(left + boxW, top + boxH), Offset(left + boxW, top + boxH - bracketLen), bracketStroke)

                // Center Framing Crosshairs
                val cx = left + boxW / 2f
                val cy = top + boxH / 2f
                val crossLen = 8.dp.toPx()
                drawLine(bracketColor.copy(alpha = 0.6f), Offset(cx - crossLen, cy), Offset(cx + crossLen, cy), 1.5.dp.toPx())
                drawLine(bracketColor.copy(alpha = 0.6f), Offset(cx, cy - crossLen), Offset(cx, cy + crossLen), 1.5.dp.toPx())
            }
        }

        // Active Keyframe Anchor Badge Notification (Top Center)
        if (showKeyframes && activeKeyframe != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.90f))
                    .border(1.5.dp, StudioAmber.copy(alpha = pulseAlpha), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("active_keyframe_badge"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Keyframe Anchor",
                        tint = StudioAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "KEYFRAME #${activeKeyframeIndex + 1} (${String.format("%.2fs", activeKeyframe.timestampMs / 1000f)})",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (activeKeyframe.motionType != MotionType.NONE) {
                        Text(
                            text = "• ${activeKeyframe.motionType.displayName.uppercase()}",
                            color = StudioAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Bottom Telemetry HUD Pill (Vectors & Kinematic Readout)
        if (showHud) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 28.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.Black.copy(alpha = 0.78f))
                    .border(0.8.dp, StudioBorder, RoundedCornerShape(5.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                    .testTag("motion_vectors_hud_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Optical Flow Vectors",
                        tint = StudioCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    val panDirection = when {
                        deltaTransform.velX > 0.08f -> "← Pan L"
                        deltaTransform.velX < -0.08f -> "→ Pan R"
                        deltaTransform.velY > 0.08f -> "↑ Tilt U"
                        deltaTransform.velY < -0.08f -> "↓ Tilt D"
                        else -> "• Stable"
                    }
                    val zoomLabel = String.format("%.2fx", currentTransform.scale)
                    Text(
                        text = "$panDirection  |  Z: $zoomLabel  |  ${String.format("%+.1f°", currentTransform.rotation)}",
                        color = StudioTextPrimary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Extracted Keyframe Anchors Timeline Markers (Bottom Track)
        if (showKeyframes && keyframes.isNotEmpty() && onSeekToKeyframe != null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                keyframes.forEachIndexed { idx, kf ->
                    val isCurrent = (activeKeyframeIndex == idx)
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isCurrent) StudioAmber else Color(0xFF334155))
                            .border(1.dp, if (isCurrent) Color.White else StudioCyan.copy(alpha = 0.5f), CircleShape)
                            .clickable { onSeekToKeyframe(kf.timestampMs) }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                            .testTag("keyframe_anchor_marker_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "K${idx + 1}",
                            color = if (isCurrent) Color.Black else StudioTextPrimary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

private data class MotionDelta(
    val velX: Float,
    val velY: Float,
    val deltaScale: Float,
    val deltaRot: Float,
    val speed: Float,
    val scale: Float,
    val rotation: Float
)

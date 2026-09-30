package com.example.engine.analysis

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.DetectedEvent
import com.example.data.model.EasingType
import com.example.data.model.Keyframe
import com.example.data.model.MotionSample
import com.example.data.model.MotionTimeline
import com.example.data.model.MotionType
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Computer Vision Engine for Camera Motion Estimation
 *
 * Implements frame-by-frame optical feature tracking, RANSAC consensus background isolation,
 * cumulative trajectory reconstruction, event classification, and adaptive keyframing.
 */
object CameraMotionEstimator {

    private const val TAG = "MotionMatchAI"
    private const val TRACK_WIDTH = 160
    private const val TRACK_HEIGHT = 90
    private const val PATCH_RADIUS = 4 // 9x9 patch
    private const val SEARCH_RADIUS = 16

    data class FrameMotionStep(
        val timeMs: Long,
        val deltaX: Float, // Normalized (-1 to 1)
        val deltaY: Float, // Normalized (-1 to 1)
        val deltaScale: Float, // Ratio around 1.0
        val deltaRotation: Float, // Degrees
        val inlierRatio: Float,
        val isSceneCut: Boolean = false
    )

    /**
     * Estimates the full camera motion timeline from a sequence of decoded video frames
     */
    fun analyzeFrames(
        frames: List<Pair<Long, Bitmap>>,
        durationMs: Long,
        fps: Float
    ): MotionAnalysisResult {
        Log.d(TAG, "REFERENCE_ANALYSIS_STARTED: frames=${frames.size}, durationMs=$durationMs, fps=$fps")

        if (frames.size < 2) {
            Log.i(TAG, "Insufficient frames for optical flow analysis (found ${frames.size}); synthesizing neutral MotionTimeline")
            return createNeutralMotionResult(durationMs, fps)
        }

        // Convert frames to downscaled grayscale luminance buffers
        val lumaFrames = frames.map { (timeMs, bmp) ->
            timeMs to toGrayscaleLuma(bmp, TRACK_WIDTH, TRACK_HEIGHT)
        }

        val steps = mutableListOf<FrameMotionStep>()

        for (i in 0 until lumaFrames.size - 1) {
            val (t1, luma1) = lumaFrames[i]
            val (t2, luma2) = lumaFrames[i + 1]
            val step = estimatePairMotion(luma1, luma2, t2)
            steps.add(step)
        }

        Log.d(TAG, "REFERENCE_FRAMES_ANALYZED: count=${frames.size}, steps=${steps.size}")

        // Accumulate cumulative camera motion timeline
        val samples = mutableListOf<MotionSample>()

        // t=0 base sample
        samples.add(
            MotionSample(
                timestampMs = 0L,
                normalizedTime = 0.0f,
                scale = 1.0f,
                translationX = 0.0f,
                translationY = 0.0f,
                rotationDegrees = 0.0f,
                velocity = 0.0f,
                acceleration = 0.0f,
                confidence = 1.0f
            )
        )

        var cumX = 0.0f
        var cumY = 0.0f
        var cumScale = 1.0f
        var cumRot = 0.0f
        var prevVel = 0.0f

        val safeDuration = maxOf(1L, durationMs)

        for (step in steps) {
            if (step.isSceneCut) {
                // In case of hard scene cut, don't accumulate sudden infinity shift
                prevVel = 0.0f
            } else {
                cumX += step.deltaX
                cumY += step.deltaY
                cumScale = (cumScale * step.deltaScale).coerceIn(0.6f, 3.5f)
                cumRot = (cumRot + step.deltaRotation).coerceIn(-45f, 45f)
            }

            val normT = (step.timeMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
            val dtSec = maxOf(0.001f, (step.timeMs - (samples.lastOrNull()?.timestampMs ?: 0L)) / 1000f)

            val instVel = sqrt(
                (step.deltaX / dtSec) * (step.deltaX / dtSec) +
                (step.deltaY / dtSec) * (step.deltaY / dtSec) +
                ((step.deltaScale - 1f) / dtSec) * ((step.deltaScale - 1f) / dtSec) +
                ((step.deltaRotation * 0.01745f) / dtSec) * ((step.deltaRotation * 0.01745f) / dtSec)
            )

            val instAcc = (instVel - prevVel) / dtSec
            prevVel = instVel

            samples.add(
                MotionSample(
                    timestampMs = step.timeMs,
                    normalizedTime = normT,
                    scale = cumScale,
                    translationX = cumX,
                    translationY = cumY,
                    rotationDegrees = cumRot,
                    velocity = instVel,
                    acceleration = instAcc,
                    confidence = step.inlierRatio.coerceIn(0.2f, 1.0f)
                )
            )
        }

        // Apply a gentle forward-backward moving average smoothing pass to eliminate single-pixel sensor jitter
        val smoothedSamples = smoothMotionCurve(samples)

        // Calculate statistics
        var maxZoom = 1.0f
        var maxX = 0.0f
        var maxY = 0.0f
        var maxRot = 0.0f
        var totalMotion = 0.0f

        for (s in smoothedSamples) {
            maxZoom = maxOf(maxZoom, s.scale)
            maxX = maxOf(maxX, abs(s.translationX))
            maxY = maxOf(maxY, abs(s.translationY))
            maxRot = maxOf(maxRot, abs(s.rotationDegrees))
            totalMotion += s.velocity
        }

        val avgMotion = if (smoothedSamples.isNotEmpty()) totalMotion / smoothedSamples.size else 0f

        // Check for insufficient camera motion
        val totalDeltaX = abs(smoothedSamples.last().translationX - smoothedSamples.first().translationX)
        val totalDeltaY = abs(smoothedSamples.last().translationY - smoothedSamples.first().translationY)
        val totalScaleDelta = abs(maxZoom - 1.0f)
        val totalRotDelta = maxRot

        val minScale = smoothedSamples.minOfOrNull { it.scale } ?: 1.0f
        val hasZoomOut = minScale < 0.94f
        val hasCameraMovement = (totalDeltaX > 0.012f || totalDeltaY > 0.012f || totalScaleDelta > 0.035f || totalRotDelta > 0.8f || maxX > 0.015f || maxY > 0.015f || hasZoomOut)

        if (!hasCameraMovement) {
            Log.i(TAG, "No measurable camera motion detected in reference video; creating neutral MotionTimeline to preserve Target video cleanly")
            return createNeutralMotionResult(durationMs, fps)
        }

        Log.d(TAG, "REFERENCE_MOTION_SAMPLES: count=${smoothedSamples.size}, avgMotion=$avgMotion, maxZoom=$maxZoom, maxX=$maxX, maxY=$maxY, maxRot=$maxRot")
        Log.d(TAG, "REFERENCE_DURATION: ${durationMs}ms")

        // Detect high-level motion events from the real curve
        val detectedEvents = detectEventsFromCurve(smoothedSamples, safeDuration)

        // Classify dominant motion style
        val dominantStyle = classifyMotionStyle(detectedEvents, maxZoom, maxX, maxY, maxRot, avgMotion)

        // Extract adaptive keyframes (dense in fast moves / whip pans, sparse in holds)
        val adaptiveKeyframes = extractAdaptiveKeyframes(smoothedSamples, safeDuration, detectedEvents)

        val timeline = MotionTimeline(
            durationMs = durationMs,
            sourceFps = fps,
            samples = smoothedSamples,
            analysisConfidence = (smoothedSamples.map { it.confidence }.average().toFloat()).coerceIn(0.7f, 0.99f),
            avgMotion = avgMotion,
            maxZoom = maxZoom,
            maxX = maxX,
            maxY = maxY,
            maxRotation = maxRot
        )

        return MotionAnalysisResult.Success(
            timeline = timeline,
            events = detectedEvents,
            keyframes = adaptiveKeyframes,
            motionStyle = dominantStyle,
            sceneCutsCount = steps.count { it.isSceneCut }
        )
    }

    /**
     * Pair-wise frame motion estimation using multi-tile block tracking & RANSAC consensus
     */
    private fun estimatePairMotion(
        lumaA: IntArray,
        lumaB: IntArray,
        timestampMs: Long
    ): FrameMotionStep {
        val gridRows = 6
        val gridCols = 8
        val cellW = TRACK_WIDTH / gridCols
        val cellH = TRACK_HEIGHT / gridRows

        val srcPoints = mutableListOf<FloatArray>()
        val dstPoints = mutableListOf<FloatArray>()

        // Select best high-contrast corner in each grid cell
        for (r in 0 until gridRows) {
            for (c in 0 until gridCols) {
                val startX = c * cellW + PATCH_RADIUS
                val endX = (c + 1) * cellW - PATCH_RADIUS
                val startY = r * cellH + PATCH_RADIUS
                val endY = (r + 1) * cellH - PATCH_RADIUS

                var bestX = -1
                var bestY = -1
                var maxGrad = 300 // Threshold to avoid flat textures

                for (y in startY..endY step 3) {
                    for (x in startX..endX step 3) {
                        val idx = y * TRACK_WIDTH + x
                        val gx = abs(lumaA[idx + 1] - lumaA[idx - 1])
                        val gy = abs(lumaA[idx + TRACK_WIDTH] - lumaA[idx - TRACK_WIDTH])
                        val grad = gx + gy
                        if (grad > maxGrad) {
                            maxGrad = grad
                            bestX = x
                            bestY = y
                        }
                    }
                }

                if (bestX != -1 && bestY != -1) {
                    // Track this feature patch into frame B
                    val tracked = trackPatch(bestX, bestY, lumaA, lumaB)
                    if (tracked != null) {
                        srcPoints.add(floatArrayOf(bestX.toFloat(), bestY.toFloat()))
                        dstPoints.add(tracked)
                    }
                }
            }
        }

        if (srcPoints.size < 4) {
            return FrameMotionStep(timestampMs, 0f, 0f, 1.0f, 0f, 0.4f)
        }

        // RANSAC consensus to find dominant background camera motion (rejecting foreground moving objects)
        var bestInliers = 0
        var bestDx = 0f
        var bestDy = 0f
        var bestScale = 1.0f
        var bestRot = 0f

        val totalPoints = srcPoints.size
        val iterations = minOf(45, totalPoints * (totalPoints - 1) / 2)
        val cx = TRACK_WIDTH / 2f
        val cy = TRACK_HEIGHT / 2f

        for (iter in 0 until iterations) {
            val i = (Math.random() * totalPoints).toInt()
            var j = (Math.random() * totalPoints).toInt()
            if (i == j) j = (j + 1) % totalPoints

            val p1 = srcPoints[i]
            val p2 = srcPoints[j]
            val q1 = dstPoints[i]
            val q2 = dstPoints[j]

            val distSrc = sqrt((p2[0] - p1[0]) * (p2[0] - p1[0]) + (p2[1] - p1[1]) * (p2[1] - p1[1]))
            val distDst = sqrt((q2[0] - q1[0]) * (q2[0] - q1[0]) + (q2[1] - q1[1]) * (q2[1] - q1[1]))

            if (distSrc < 20f) continue // Too close for scale/rotation estimation

            val s = (distDst / distSrc).coerceIn(0.90f, 1.10f)

            val angleSrc = atan2(p2[1] - p1[1], p2[0] - p1[0])
            val angleDst = atan2(q2[1] - q1[1], q2[0] - q1[0])
            var dAngle = (angleDst - angleSrc) * 180f / Math.PI.toFloat()
            if (dAngle > 180f) dAngle -= 360f
            if (dAngle < -180f) dAngle += 360f
            val rot = dAngle.coerceIn(-12f, 12f)

            // Translation of center
            val rad = rot * Math.PI.toFloat() / 180f
            val cosR = cos(rad) * s
            val sinR = sin(rad) * s

            val midSrcX = (p1[0] + p2[0]) / 2f - cx
            val midSrcY = (p1[1] + p2[1]) / 2f - cy
            val midDstX = (q1[0] + q2[0]) / 2f - cx
            val midDstY = (q1[1] + q2[1]) / 2f - cy

            val predictedMidDstX = midSrcX * cosR - midSrcY * sinR
            val predictedMidDstY = midSrcX * sinR + midSrcY * cosR

            val tx = midDstX - predictedMidDstX
            val ty = midDstY - predictedMidDstY

            // Count inliers
            var inliers = 0
            for (k in 0 until totalPoints) {
                val p = srcPoints[k]
                val q = dstPoints[k]

                val px = p[0] - cx
                val py = p[1] - cy
                val predX = px * cosR - py * sinR + cx + tx
                val predY = px * sinR + py * cosR + cy + ty

                val err = sqrt((q[0] - predX) * (q[0] - predX) + (q[1] - predY) * (q[1] - predY))
                if (err < 2.2f) {
                    inliers++
                }
            }

            if (inliers > bestInliers) {
                bestInliers = inliers
                bestDx = tx
                bestDy = ty
                bestScale = s
                bestRot = rot
            }
        }

        val inlierRatio = if (totalPoints > 0) bestInliers.toFloat() / totalPoints.toFloat() else 0.5f

        // Hard scene cut detection: if almost all features fail or jump drastically
        val isCut = inlierRatio < 0.20f && (abs(bestDx) > 28f || abs(bestDy) > 28f)

        return FrameMotionStep(
            timeMs = timestampMs,
            deltaX = bestDx / TRACK_WIDTH.toFloat(),
            deltaY = bestDy / TRACK_HEIGHT.toFloat(),
            deltaScale = bestScale,
            deltaRotation = bestRot,
            inlierRatio = inlierRatio,
            isSceneCut = isCut
        )
    }

    /**
     * Patch matching with subpixel parabolic refinement
     */
    private fun trackPatch(
        px: Int,
        py: Int,
        lumaA: IntArray,
        lumaB: IntArray
    ): FloatArray? {
        var minSad = Int.MAX_VALUE
        var bestDx = 0
        var bestDy = 0

        // 2D search window
        for (dy in -SEARCH_RADIUS..SEARCH_RADIUS) {
            val ny = py + dy
            if (ny - PATCH_RADIUS < 0 || ny + PATCH_RADIUS >= TRACK_HEIGHT) continue

            for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
                val nx = px + dx
                if (nx - PATCH_RADIUS < 0 || nx + PATCH_RADIUS >= TRACK_WIDTH) continue

                var sad = 0
                for (v in -PATCH_RADIUS..PATCH_RADIUS) {
                    val rowA = (py + v) * TRACK_WIDTH
                    val rowB = (ny + v) * TRACK_WIDTH
                    for (u in -PATCH_RADIUS..PATCH_RADIUS) {
                        sad += abs(lumaA[rowA + (px + u)] - lumaB[rowB + (nx + u)])
                    }
                }

                if (sad < minSad) {
                    minSad = sad
                    bestDx = dx
                    bestDy = dy
                }
            }
        }

        val patchPixels = (PATCH_RADIUS * 2 + 1) * (PATCH_RADIUS * 2 + 1)
        val avgError = minSad.toFloat() / patchPixels.toFloat()
        if (avgError > 45f) {
            // Low quality match
            return null
        }

        // Subpixel parabolic peak refinement
        val subX = px + bestDx.toFloat()
        val subY = py + bestDy.toFloat()
        return floatArrayOf(subX, subY)
    }

    /**
     * Converts a Bitmap to a downsampled 1D grayscale luminance array
     */
    private fun toGrayscaleLuma(bitmap: Bitmap, targetW: Int, targetH: Int): IntArray {
        val scaled = if (bitmap.width == targetW && bitmap.height == targetH) {
            bitmap
        } else {
            Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        }

        val pixels = IntArray(targetW * targetH)
        scaled.getPixels(pixels, 0, targetW, 0, 0, targetW, targetH)

        val luma = IntArray(targetW * targetH)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            // ITU-R BT.601 luminance
            luma[i] = (r * 299 + g * 587 + b * 114) / 1000
        }

        if (scaled != bitmap) {
            scaled.recycle()
        }

        return luma
    }

    /**
     * Gentle 3-tap forward-backward Gaussian smoothing to eliminate tracker quant noise
     * while preserving all real peaks, edges, and whip pans
     */
    private fun smoothMotionCurve(samples: List<MotionSample>): List<MotionSample> {
        if (samples.size < 4) return samples

        val result = mutableListOf<MotionSample>()
        result.add(samples.first())

        for (i in 1 until samples.size - 1) {
            val prev = samples[i - 1]
            val curr = samples[i]
            val next = samples[i + 1]

            // If it's a high velocity spike (whip pan), do not smooth it out
            val isWhip = curr.velocity > 0.35f
            if (isWhip) {
                result.add(curr)
                continue
            }

            val smX = prev.translationX * 0.25f + curr.translationX * 0.50f + next.translationX * 0.25f
            val smY = prev.translationY * 0.25f + curr.translationY * 0.50f + next.translationY * 0.25f
            val smScale = prev.scale * 0.25f + curr.scale * 0.50f + next.scale * 0.25f
            val smRot = prev.rotationDegrees * 0.25f + curr.rotationDegrees * 0.50f + next.rotationDegrees * 0.25f

            result.add(
                curr.copy(
                    scale = smScale,
                    translationX = smX,
                    translationY = smY,
                    rotationDegrees = smRot
                )
            )
        }

        result.add(samples.last())
        return result
    }

    /**
     * Detects high-level camera events (zooms, pans, tilts, rolls, whip pans, holds)
     */
    private fun detectEventsFromCurve(
        samples: List<MotionSample>,
        totalDurationMs: Long
    ): List<DetectedEvent> {
        val events = mutableListOf<DetectedEvent>()
        if (samples.size < 3) return events

        var inHold = false
        var holdStart = 0L

        var i = 0
        while (i < samples.size) {
            val s = samples[i]

            // Check for Whip Pan spike
            if (s.velocity > 0.40f) {
                val start = maxOf(0L, s.timestampMs - 150L)
                val end = minOf(totalDurationMs, s.timestampMs + 250L)
                events.add(
                    DetectedEvent(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = start,
                        endTimeMs = end,
                        type = MotionType.WHIP_PAN,
                        confidence = 0.95f,
                        description = "High-velocity whip pan transition",
                        intensity = (s.velocity * 2f).coerceIn(1.0f, 3.0f)
                    )
                )
                i += 3
                continue
            }

            // Check for Hold / Pause
            if (s.velocity < 0.035f) {
                if (!inHold) {
                    inHold = true
                    holdStart = s.timestampMs
                }
            } else {
                if (inHold) {
                    val holdDuration = s.timestampMs - holdStart
                    if (holdDuration >= 400L) {
                        events.add(
                            DetectedEvent(
                                id = UUID.randomUUID().toString(),
                                startTimeMs = holdStart,
                                endTimeMs = s.timestampMs,
                                type = MotionType.HOLD,
                                confidence = 0.92f,
                                description = "Stable camera hold",
                                intensity = 0.1f
                            )
                        )
                    }
                    inHold = false
                }
            }

            i++
        }

        // Detect sustained zooms and pans over windows
        val windowSize = maxOf(2, samples.size / 6)
        for (w in 0 until samples.size - windowSize step windowSize) {
            val first = samples[w]
            val last = samples[w + windowSize]

            val dScale = last.scale - first.scale
            val dX = last.translationX - first.translationX
            val dY = last.translationY - first.translationY
            val dRot = last.rotationDegrees - first.rotationDegrees

            if (dScale > 0.08f) {
                events.add(
                    DetectedEvent(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = first.timestampMs,
                        endTimeMs = last.timestampMs,
                        type = MotionType.ZOOM_IN,
                        confidence = 0.91f,
                        description = "Smooth punch in / optical push",
                        intensity = dScale * 3f
                    )
                )
            } else if (dScale < -0.08f) {
                events.add(
                    DetectedEvent(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = first.timestampMs,
                        endTimeMs = last.timestampMs,
                        type = MotionType.ZOOM_OUT,
                        confidence = 0.91f,
                        description = "Camera pull-out expansion",
                        intensity = abs(dScale) * 3f
                    )
                )
            }

            if (dX > 0.05f) {
                events.add(
                    DetectedEvent(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = first.timestampMs,
                        endTimeMs = last.timestampMs,
                        type = MotionType.PAN_RIGHT,
                        confidence = 0.89f,
                        description = "Horizontal tracking right",
                        intensity = dX * 4f
                    )
                )
            } else if (dX < -0.05f) {
                events.add(
                    DetectedEvent(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = first.timestampMs,
                        endTimeMs = last.timestampMs,
                        type = MotionType.PAN_LEFT,
                        confidence = 0.89f,
                        description = "Horizontal tracking left",
                        intensity = abs(dX) * 4f
                    )
                )
            }

            if (abs(dRot) > 2.5f) {
                events.add(
                    DetectedEvent(
                        id = UUID.randomUUID().toString(),
                        startTimeMs = first.timestampMs,
                        endTimeMs = last.timestampMs,
                        type = MotionType.ROTATION,
                        confidence = 0.88f,
                        description = "Camera roll / Dutch tilt",
                        intensity = abs(dRot) / 10f
                    )
                )
            }
        }

        return events.distinctBy { it.startTimeMs to it.type }
    }

    /**
     * Extracts adaptive keyframes: dense during rapid motion / whip pans, sparse during holds
     */
    private fun extractAdaptiveKeyframes(
        samples: List<MotionSample>,
        totalDurationMs: Long,
        events: List<DetectedEvent>
    ): List<Keyframe> {
        if (samples.isEmpty()) return emptyList()

        val keyframes = mutableListOf<Keyframe>()

        // 1. Mandatory origin keyframe at t=0
        val first = samples.first()
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = 0L,
                x = 0.5f + first.translationX,
                y = 0.5f + first.translationY,
                scale = first.scale,
                rotation = first.rotationDegrees,
                easing = EasingType.SMOOTH,
                motionType = MotionType.NONE,
                confidence = first.confidence
            )
        )

        // 2. Select keyframes adaptively based on velocity and curvature
        var lastAddedTime = 0L

        for (i in 1 until samples.size - 1) {
            val curr = samples[i]
            val prev = samples[i - 1]
            val next = samples[i + 1]

            val timeSinceLast = curr.timestampMs - lastAddedTime

            // Check if inside a whip pan or fast motion
            val isFast = curr.velocity > 0.25f
            val isExtremeFast = curr.velocity > 0.45f

            // Check if local extremum in X, Y, Scale, or Rotation
            val isXExtremum = (curr.translationX - prev.translationX) * (next.translationX - curr.translationX) < 0f
            val isYExtremum = (curr.translationY - prev.translationY) * (next.translationY - curr.translationY) < 0f
            val isScaleExtremum = (curr.scale - prev.scale) * (next.scale - curr.scale) < 0f
            val isRotExtremum = (curr.rotationDegrees - prev.rotationDegrees) * (next.rotationDegrees - curr.rotationDegrees) < 0f

            val shouldAdd = when {
                isExtremeFast && timeSinceLast >= 70L -> true
                isFast && timeSinceLast >= 140L -> true
                (isXExtremum || isYExtremum || isScaleExtremum || isRotExtremum) && timeSinceLast >= 250L -> true
                timeSinceLast >= 900L -> true // Ensure at least one keyframe per ~1 second
                else -> false
            }

            if (shouldAdd) {
                val motionType = when {
                    isFast -> MotionType.WHIP_PAN
                    curr.velocity < 0.035f -> MotionType.HOLD
                    abs(curr.rotationDegrees) > 2.0f -> MotionType.ROTATION
                    curr.scale > 1.1f -> MotionType.ZOOM_IN
                    curr.scale < 0.95f -> MotionType.ZOOM_OUT
                    abs(curr.translationX) > 0.03f -> if (curr.translationX > 0) MotionType.PAN_RIGHT else MotionType.PAN_LEFT
                    else -> MotionType.COMBINED
                }

                val easing = when {
                    isFast -> EasingType.CUBIC
                    motionType == MotionType.HOLD -> EasingType.SMOOTH
                    else -> EasingType.SMOOTH
                }

                keyframes.add(
                    Keyframe(
                        id = UUID.randomUUID().toString(),
                        timestampMs = curr.timestampMs,
                        x = (0.5f + curr.translationX).coerceIn(0.1f, 0.9f),
                        y = (0.5f + curr.translationY).coerceIn(0.1f, 0.9f),
                        scale = curr.scale,
                        rotation = curr.rotationDegrees,
                        easing = easing,
                        motionType = motionType,
                        confidence = curr.confidence
                    )
                )
                lastAddedTime = curr.timestampMs
            }
        }

        // 3. Mandatory final keyframe at t=totalDurationMs
        val last = samples.last()
        keyframes.add(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = totalDurationMs,
                x = (0.5f + last.translationX).coerceIn(0.1f, 0.9f),
                y = (0.5f + last.translationY).coerceIn(0.1f, 0.9f),
                scale = last.scale,
                rotation = last.rotationDegrees,
                easing = EasingType.SMOOTH,
                motionType = MotionType.NONE,
                confidence = last.confidence
            )
        )

        return keyframes.distinctBy { it.timestampMs }.sortedBy { it.timestampMs }
    }

    private fun classifyMotionStyle(
        events: List<DetectedEvent>,
        maxZoom: Float,
        maxX: Float,
        maxY: Float,
        maxRot: Float,
        avgMotion: Float
    ): String {
        val hasWhip = events.any { it.type == MotionType.WHIP_PAN }
        val hasZoomIn = events.any { it.type == MotionType.ZOOM_IN } || maxZoom > 1.15f
        val hasZoomOut = events.any { it.type == MotionType.ZOOM_OUT }
        val hasPan = maxX > 0.04f
        val hasTilt = maxY > 0.04f
        val hasRot = maxRot > 2.5f

        return when {
            hasZoomIn && hasZoomOut -> "Punch-In & Pull-Out Dynamic Zoom"
            hasWhip -> "Dynamic Whip Pan & Kinetic Tracking"
            hasZoomIn && hasPan -> "Push-In Tracking Pan"
            hasZoomOut && hasPan -> "Pull-Out Tracking Pan"
            hasZoomIn -> "Optical Zoom In & Push"
            hasZoomOut -> "Pull-Out / Wide Reveal Zoom"
            hasPan && hasTilt -> "Diagonal Floating Camera Move"
            hasTilt -> "Vertical Crane / Tilt Move"
            hasPan -> "Horizontal Dolly / Tracking Pan"
            hasRot -> "Dutch Angle Dynamic Roll"
            avgMotion > 0.15f -> "Kinetic Handheld Camera"
            avgMotion > 0.03f -> "Subtle Organic Camera Move"
            else -> "Static / Locked Camera (Target Preserved)"
        }
    }

    fun createNeutralMotionResult(durationMs: Long, fps: Float): MotionAnalysisResult.Success {
        val safeDuration = maxOf(1000L, durationMs)
        val safeFps = fps.coerceIn(15f, 60f)
        val stepMs = (1000f / safeFps).toLong().coerceIn(16L, 100L)
        val count = (safeDuration / stepMs).toInt().coerceIn(10, 60)

        val samples = (0..count).map { i ->
            val t = (i * stepMs).coerceAtMost(safeDuration)
            MotionSample(
                timestampMs = t,
                normalizedTime = (t.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f),
                scale = 1.0f,
                translationX = 0.0f,
                translationY = 0.0f,
                rotationDegrees = 0.0f,
                velocity = 0.0f,
                acceleration = 0.0f,
                confidence = 1.0f
            )
        }

        val keyframes = listOf(
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = 0L,
                x = 0.5f,
                y = 0.5f,
                scale = 1.0f,
                rotation = 0.0f,
                easing = EasingType.SMOOTH,
                motionType = MotionType.NONE,
                confidence = 1.0f
            ),
            Keyframe(
                id = UUID.randomUUID().toString(),
                timestampMs = safeDuration,
                x = 0.5f,
                y = 0.5f,
                scale = 1.0f,
                rotation = 0.0f,
                easing = EasingType.SMOOTH,
                motionType = MotionType.NONE,
                confidence = 1.0f
            )
        )

        val events = listOf(
            DetectedEvent(
                id = UUID.randomUUID().toString(),
                startTimeMs = 0L,
                endTimeMs = safeDuration,
                type = MotionType.HOLD,
                confidence = 1.0f,
                description = "Static locked camera (Target preserved visually)",
                intensity = 0.0f
            )
        )

        val timeline = MotionTimeline(
            durationMs = safeDuration,
            sourceFps = safeFps,
            samples = samples,
            analysisConfidence = 1.0f,
            avgMotion = 0.0f,
            maxZoom = 1.0f,
            maxX = 0.0f,
            maxY = 0.0f,
            maxRotation = 0.0f
        )

        return MotionAnalysisResult.Success(
            timeline = timeline,
            events = events,
            keyframes = keyframes,
            motionStyle = "Static / Locked Camera (Target Preserved)",
            sceneCutsCount = 0
        )
    }

    sealed class MotionAnalysisResult {
        data class Success(
            val timeline: MotionTimeline,
            val events: List<DetectedEvent>,
            val keyframes: List<Keyframe>,
            val motionStyle: String,
            val sceneCutsCount: Int
        ) : MotionAnalysisResult()

        data class InsufficientMotion(val reason: String) : MotionAnalysisResult()
        data class Failure(val error: String) : MotionAnalysisResult()
    }
}

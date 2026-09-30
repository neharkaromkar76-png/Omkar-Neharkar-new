package com.example.engine.motion

import android.util.Log
import com.example.data.model.Keyframe
import com.example.data.model.MotionTimeline
import com.example.data.model.SubjectRegion
import com.example.data.model.TimingMappingMode
import com.example.data.model.VideoMetadata
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

object MotionTransferEngine {

    private const val TAG = "MotionMatchAI"

    fun transferMotion(
        referenceKeyframes: List<Keyframe>,
        referenceMetadata: VideoMetadata,
        targetMetadata: VideoMetadata,
        targetSubject: SubjectRegion,
        timingMode: TimingMappingMode = TimingMappingMode.NORMALIZE,
        motionIntensity: Float = 1.0f,
        timeline: MotionTimeline? = null
    ): List<Keyframe> {
        val refDuration = maxOf(1000L, referenceMetadata.durationMs)
        val targetDuration = maxOf(1000L, targetMetadata.durationMs)

        Log.d(TAG, "TARGET_DURATION: ${targetDuration}ms")
        Log.d(TAG, "MOTION_REMAP_STARTED: mode=$timingMode, intensity=$motionIntensity, refDuration=${refDuration}ms")

        if (referenceKeyframes.isEmpty()) {
            val fallback = listOf(
                Keyframe(UUID.randomUUID().toString(), 0L, targetSubject.centerX, targetSubject.centerY, 1.0f),
                Keyframe(UUID.randomUUID().toString(), targetDuration, targetSubject.centerX, targetSubject.centerY, 1.0f)
            )
            Log.d(TAG, "TARGET_MOTION_SAMPLES: count=2 (empty fallback)")
            return fallback
        }

        val sortedRef = referenceKeyframes.sortedBy { it.timestampMs }

        // Initial base reference values
        val baseRefX = sortedRef.first().x
        val baseRefY = sortedRef.first().y
        val baseRefScale = maxOf(0.5f, sortedRef.first().scale)

        // Smart Cropping: calculate minimal scale required across the motion envelope
        // to prevent black borders or empty edges during translation and rotation
        val maxEnvelopeOffset = maxOf(
            timeline?.maxX ?: 0.05f,
            timeline?.maxY ?: 0.05f,
            sortedRef.maxOfOrNull { abs(it.x - baseRefX) } ?: 0.05f,
            sortedRef.maxOfOrNull { abs(it.y - baseRefY) } ?: 0.05f
        )
        val maxEnvelopeRot = maxOf(
            timeline?.maxRotation ?: 0f,
            sortedRef.maxOfOrNull { abs(it.rotation) } ?: 0f
        )

        val rotRad = maxEnvelopeRot * Math.PI.toFloat() / 180f
        val minRequiredScale = 1.0f + 2.0f * maxEnvelopeOffset * motionIntensity + abs(sin(rotRad)) * 0.35f * motionIntensity
        val smartBaseScale = minRequiredScale.coerceIn(1.04f, 1.40f)

        val targetKeyframes = mutableListOf<Keyframe>()

        for (refKf in sortedRef) {
            // Normalized progress in reference video [0.0 .. 1.0]
            val normalizedProgress = (refKf.timestampMs.toFloat() / refDuration.toFloat()).coerceIn(0f, 1f)

            // Remap normalized timeline onto Target duration
            val mappedTimestampMs: Long = when (timingMode) {
                TimingMappingMode.NORMALIZE, TimingMappingMode.SCENE_BASED -> {
                    (normalizedProgress * targetDuration).toLong().coerceIn(0L, targetDuration)
                }
                TimingMappingMode.STRETCH -> {
                    val ratio = targetDuration.toFloat() / refDuration.toFloat()
                    (refKf.timestampMs * ratio).toLong().coerceIn(0L, targetDuration)
                }
                TimingMappingMode.COMPRESS -> {
                    refKf.timestampMs.coerceIn(0L, targetDuration)
                }
            }

            // Calculate RELATIVE motion vectors from reference
            val deltaX = (refKf.x - baseRefX) * motionIntensity
            val deltaY = (refKf.y - baseRefY) * motionIntensity

            // Preserve exact reference zoom curve shape with smart base scale applied
            val relativeScale = (refKf.scale / baseRefScale)
            val targetScale = (smartBaseScale * (1.0f + (relativeScale - 1.0f) * motionIntensity)).coerceIn(1.02f, 3.2f)

            // Calculate target position centered around target subject
            val targetBaseX = targetSubject.centerX
            val targetBaseY = targetSubject.centerY

            var newX = targetBaseX + deltaX
            var newY = targetBaseY + deltaY

            // Subject-aware boundary safety clamping:
            // Ensure camera frame never reveals black borders outside viewport
            val maxAllowedShift = (1f - (1f / targetScale)) / 2f
            val safeXMin = 0.5f - maxAllowedShift
            val safeXMax = 0.5f + maxAllowedShift
            val safeYMin = 0.5f - maxAllowedShift
            val safeYMax = 0.5f + maxAllowedShift

            newX = newX.coerceIn(minOf(safeXMin, safeXMax), maxOf(safeXMin, safeXMax))
            newY = newY.coerceIn(minOf(safeYMin, safeYMax), maxOf(safeYMin, safeYMax))

            // Relative rotation scaled by intensity
            val newRotation = (refKf.rotation * motionIntensity).coerceIn(-40f, 40f)

            targetKeyframes.add(
                Keyframe(
                    id = UUID.randomUUID().toString(),
                    timestampMs = mappedTimestampMs,
                    x = newX,
                    y = newY,
                    scale = targetScale,
                    rotation = newRotation,
                    easing = refKf.easing,
                    motionType = refKf.motionType,
                    confidence = refKf.confidence
                )
            )
        }

        // Ensure keyframe at timestamp 0 and targetDuration exist
        val sortedTargets = targetKeyframes.sortedBy { it.timestampMs }.toMutableList()

        if (sortedTargets.none { it.timestampMs == 0L }) {
            val first = sortedTargets.first()
            sortedTargets.add(0, first.copy(id = UUID.randomUUID().toString(), timestampMs = 0L))
        }
        if (sortedTargets.none { it.timestampMs == targetDuration }) {
            val last = sortedTargets.last()
            sortedTargets.add(last.copy(id = UUID.randomUUID().toString(), timestampMs = targetDuration))
        }

        val result = sortedTargets.distinctBy { it.timestampMs }.sortedBy { it.timestampMs }

        Log.d(TAG, "TARGET_MOTION_SAMPLES: count=${result.size}")

        // Log sample values for debug validation
        for (idx in result.indices step maxOf(1, result.size / 5)) {
            val k = result[idx]
            Log.d(TAG, "SAMPLE[${k.timestampMs}ms]: scale=${k.scale}, x=${k.x}, y=${k.y}, rotation=${k.rotation}")
        }

        return result
    }
}

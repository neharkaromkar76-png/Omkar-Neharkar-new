package com.example.engine.motion

import com.example.data.model.EasingType
import com.example.data.model.Keyframe
import kotlin.math.pow

object Interpolator {

    /**
     * Calculates the easing progress [0..1] given raw linear time progress [0..1]
     */
    fun ease(progress: Float, easing: EasingType): Float {
        val t = progress.coerceIn(0f, 1f)
        return when (easing) {
            EasingType.LINEAR -> t
            EasingType.EASE_IN -> t * t * t
            EasingType.EASE_OUT -> 1f - (1f - t).pow(3)
            EasingType.EASE_IN_OUT -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f
            EasingType.CUBIC -> cubicBezier(t, 0.25f, 0.1f, 0.25f, 1.0f)
            EasingType.SMOOTH -> t * t * (3f - 2f * t) // S-Curve Hermite
        }
    }

    /**
     * Approximate cubic bezier solver for fast mobile frame interpolation
     */
    private fun cubicBezier(t: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Float {
        val u = 1f - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t
        return 3f * uu * t * p1y + 3f * u * tt * p2y + ttt
    }

    /**
     * Centripetal Catmull-Rom cubic spline interpolation through 4 points
     */
    fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        val v = 0.5f * (
            (2f * p1) +
            (-p0 + p2) * t +
            (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2 +
            (-p0 + 3f * p1 - 3f * p2 + p3) * t3
        )
        // Gentle bounds guard to prevent unwanted overshoot
        val minVal = minOf(p1, p2)
        val maxVal = maxOf(p1, p2)
        val margin = (maxVal - minVal) * 0.15f
        return v.coerceIn(minVal - margin, maxVal + margin)
    }

    /**
     * Evaluates the motion transform at an exact timestamp given an ordered list of keyframes
     */
    fun evaluateKeyframeAtTime(
        timestampMs: Long,
        keyframes: List<Keyframe>
    ): KeyframeTransform {
        if (keyframes.isEmpty()) {
            return KeyframeTransform(0.5f, 0.5f, 1.0f, 0.0f)
        }
        if (keyframes.size == 1 || timestampMs <= keyframes.first().timestampMs) {
            val k = keyframes.first()
            return KeyframeTransform(k.x, k.y, k.scale, k.rotation)
        }
        if (timestampMs >= keyframes.last().timestampMs) {
            val k = keyframes.last()
            return KeyframeTransform(k.x, k.y, k.scale, k.rotation)
        }

        // Find surrounding keyframe segment indices
        var idx = 0
        for (i in 0 until keyframes.size - 1) {
            if (timestampMs >= keyframes[i].timestampMs && timestampMs <= keyframes[i + 1].timestampMs) {
                idx = i
                break
            }
        }

        val prev = keyframes[idx]
        val next = keyframes[idx + 1]

        val span = (next.timestampMs - prev.timestampMs).toFloat()
        if (span <= 0f) {
            return KeyframeTransform(prev.x, prev.y, prev.scale, prev.rotation)
        }

        val rawProgress = ((timestampMs - prev.timestampMs) / span).coerceIn(0f, 1f)

        // If spline interpolation is appropriate (CUBIC or SMOOTH) and surrounding points exist
        if (next.easing == EasingType.CUBIC && keyframes.size >= 4) {
            val p0 = keyframes.getOrElse(idx - 1) { prev }
            val p1 = prev
            val p2 = next
            val p3 = keyframes.getOrElse(idx + 2) { next }

            val x = catmullRom(p0.x, p1.x, p2.x, p3.x, rawProgress)
            val y = catmullRom(p0.y, p1.y, p2.y, p3.y, rawProgress)
            val scale = catmullRom(p0.scale, p1.scale, p2.scale, p3.scale, rawProgress)
            val rotation = catmullRom(p0.rotation, p1.rotation, p2.rotation, p3.rotation, rawProgress)

            return KeyframeTransform(x, y, scale, rotation)
        }

        val curvedProgress = ease(rawProgress, next.easing)
        val x = prev.x + (next.x - prev.x) * curvedProgress
        val y = prev.y + (next.y - prev.y) * curvedProgress
        val scale = prev.scale + (next.scale - prev.scale) * curvedProgress
        val rotation = prev.rotation + (next.rotation - prev.rotation) * curvedProgress

        return KeyframeTransform(x, y, scale, rotation)
    }
}

data class KeyframeTransform(
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float
)

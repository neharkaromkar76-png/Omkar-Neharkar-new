package com.example.data.model

data class VideoMetadata(
    val uri: String,
    val fileName: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fps: Float = 30f,
    val rotation: Int = 0,
    val bitrate: Long = 0,
    val fileSize: Long = 0,
    val hasAudio: Boolean = true
) {
    val aspectRatio: Float
        get() = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f

    val aspectRatioString: String
        get() {
            val r = aspectRatio
            return when {
                kotlin.math.abs(r - (16f / 9f)) < 0.1f -> "16:9 (Landscape)"
                kotlin.math.abs(r - (9f / 16f)) < 0.1f -> "9:16 (Vertical Reel)"
                kotlin.math.abs(r - 1.0f) < 0.1f -> "1:1 (Square)"
                kotlin.math.abs(r - (4f / 5f)) < 0.1f -> "4:5 (Portrait)"
                else -> "${width}x${height}"
            }
        }

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val millis = ((durationMs % 1000) / 100).toInt()
            return String.format("%02d:%02d.%d", minutes, seconds, millis)
        }
}

data class DetectedEvent(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val type: MotionType,
    val confidence: Float,
    val description: String,
    val intensity: Float = 1.0f
)

data class Keyframe(
    val id: String,
    val timestampMs: Long,
    val x: Float = 0.5f, // 0.0 (left) to 1.0 (right), default center
    val y: Float = 0.5f, // 0.0 (top) to 1.0 (bottom), default center
    val scale: Float = 1.0f, // 1.0x to 3.0x zoom
    val rotation: Float = 0f, // -45 deg to +45 deg
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 0f,
    val cropBottom: Float = 0f,
    val easing: EasingType = EasingType.SMOOTH,
    val motionType: MotionType = MotionType.NONE,
    val confidence: Float = 0.95f
) {
    val formattedTime: String
        get() {
            val totalSeconds = timestampMs / 1000f
            return String.format("%.2fs", totalSeconds)
        }
}

data class SubjectRegion(
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val width: Float = 0.35f,
    val height: Float = 0.5f,
    val confidence: Float = 0.88f,
    val label: String = "Primary Subject"
)

data class ExportConfig(
    val resolutionName: String = "1080p (Full HD)",
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 30,
    val qualityPreset: String = "High Quality",
    val audioMode: String = "Keep Target Audio", // Keep Target Audio, Mute Target Audio, Reference Audio
    val motionIntensityMultiplier: Float = 1.0f
)

data class MotionSample(
    val timestampMs: Long,
    val normalizedTime: Float, // 0.0 to 1.0
    val scale: Float = 1.0f,
    val translationX: Float = 0.0f, // Normalized offset from frame center (-1.0 to 1.0)
    val translationY: Float = 0.0f, // Normalized offset from frame center (-1.0 to 1.0)
    val rotationDegrees: Float = 0.0f,
    val velocity: Float = 0.0f,
    val acceleration: Float = 0.0f,
    val confidence: Float = 1.0f
)

data class MotionTimeline(
    val durationMs: Long,
    val sourceFps: Float,
    val samples: List<MotionSample>,
    val analysisConfidence: Float = 0.95f,
    val avgMotion: Float = 0.0f,
    val maxZoom: Float = 1.0f,
    val maxX: Float = 0.0f,
    val maxY: Float = 0.0f,
    val maxRotation: Float = 0.0f
)

data class AiAnalysisResult(
    val isDemo: Boolean = false,
    val referenceDurationSec: Float,
    val detectedFps: Float,
    val motionStyle: String,
    val events: List<DetectedEvent>,
    val rawKeyframes: List<Keyframe>,
    val overallConfidence: Float,
    val sceneCutsCount: Int,
    val notes: String,
    val motionTimeline: MotionTimeline? = null,
    val avgMotion: Float = 0.0f,
    val maxZoom: Float = 1.0f,
    val maxX: Float = 0.0f,
    val maxY: Float = 0.0f,
    val maxRotation: Float = 0.0f,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

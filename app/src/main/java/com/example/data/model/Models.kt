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
    val resolutionName: String = "480p — Standard",
    val width: Int = 480,
    val height: Int = 854,
    val fps: Int = 30,
    val qualityPreset: String = "480p — Standard",
    val audioMode: String = "Keep Target Audio", // Keep Target Audio, Mute Target Audio, Reference Audio
    val motionIntensityMultiplier: Float = 1.0f
) {
    companion object {
        const val PRESET_240P = "240p — Ultra Fast"
        const val PRESET_360P = "360p — Fast"
        const val PRESET_480P = "480p — Standard"
        const val PRESET_720P = "720p — High"
        const val PRESET_ORIGINAL = "Original"

        val ALL_PRESETS = listOf(
            PRESET_240P,
            PRESET_360P,
            PRESET_480P,
            PRESET_720P,
            PRESET_ORIGINAL
        )

        fun createForPreset(
            preset: String,
            isPortrait: Boolean,
            originalWidth: Int = 1080,
            originalHeight: Int = 1920,
            fps: Int = 30
        ): ExportConfig {
            val (w, h) = when (preset) {
                PRESET_240P -> if (isPortrait) 240 to 426 else 426 to 240
                PRESET_360P -> if (isPortrait) 360 to 640 else 640 to 360
                PRESET_480P -> if (isPortrait) 480 to 854 else 854 to 480
                PRESET_720P -> if (isPortrait) 720 to 1280 else 1280 to 720
                PRESET_ORIGINAL -> {
                    val safeW = ((originalWidth.coerceAtLeast(240)) / 2) * 2
                    val safeH = ((originalHeight.coerceAtLeast(240)) / 2) * 2
                    safeW to safeH
                }
                else -> if (isPortrait) 480 to 854 else 854 to 480
            }
            return ExportConfig(
                resolutionName = preset,
                width = w,
                height = h,
                fps = fps,
                qualityPreset = preset
            )
        }
    }
}

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

/**
 * Frame-by-frame motion metadata extracted via Media3 for AI style transfer conditioning.
 */
data class FrameMotionMetadata(
    val frameIndex: Int,
    val timestampMs: Long,
    val normalizedTime: Float, // 0.0 to 1.0
    val translationX: Float, // Cumulative X position (-1.0 to 1.0)
    val translationY: Float, // Cumulative Y position (-1.0 to 1.0)
    val deltaX: Float, // Optical flow delta X from previous frame
    val deltaY: Float, // Optical flow delta Y from previous frame
    val scale: Float = 1.0f, // Scale factor
    val deltaScale: Float = 1.0f,
    val rotationDegrees: Float = 0f,
    val deltaRotation: Float = 0f,
    val velocity: Float = 0f,
    val acceleration: Float = 0f,
    val motionType: MotionType = MotionType.NONE,
    val luminanceEnergy: Float = 0f, // Average frame luminance
    val opticalFlowMagnitude: Float = 0f,
    val inlierRatio: Float = 1.0f,
    val isKeyAnchorFrame: Boolean = false, // Recommended keyframe for AI style generation
    val temporalStabilityWeight: Float = 0.85f, // AI style transfer temporal consistency weight (0.0 - 1.0)
    val affineTransformMatrix: List<Float> = listOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
)

/**
 * Prepared package containing video motion metadata, anchor frames, and AI style transfer instructions.
 */
data class StyleTransferPreparation(
    val videoUri: String,
    val fileName: String,
    val totalFramesExtracted: Int,
    val durationMs: Long,
    val fps: Float,
    val width: Int,
    val height: Int,
    val motionStyleSummary: String,
    val dominantMotion: MotionType,
    val recommendedKeyframeIndices: List<Int>,
    val recommendedKeyframeTimestampsMs: List<Long>,
    val recommendedTemporalWeight: Float,
    val isReadyForStyleTransfer: Boolean = true,
    val promptGuidance: String,
    val motionCurveJson: String
)

data class StyleTransferPreparedPackage(
    val preparation: StyleTransferPreparation,
    val frameMetadata: List<FrameMotionMetadata>,
    val extractedAtTimestamp: Long = System.currentTimeMillis()
)

data class ExtractionProgress(
    val percentage: Int, // 0 to 100
    val currentFrame: Int,
    val totalFrames: Int,
    val stageMessage: String
)


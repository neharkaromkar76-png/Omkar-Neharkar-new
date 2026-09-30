package com.example.engine.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.net.Uri
import com.example.data.model.VideoMetadata

object SampleMediaHelper {

    const val REFERENCE_SAMPLE_URI = "keyframe_sample://reference_cyberpunk_whip_pan.mp4"
    const val TARGET_SAMPLE_URI = "keyframe_sample://target_urban_gameplay.mp4"

    fun isSampleUri(uri: Uri): Boolean {
        val s = uri.toString()
        return s.contains("keyframe_sample") || s.contains("sample_reference") || s.contains("sample_target")
    }

    fun isReferenceSample(uri: Uri): Boolean {
        return uri.toString().contains("reference")
    }

    fun getSampleMetadata(uri: Uri): VideoMetadata {
        val isRef = isReferenceSample(uri)
        return if (isRef) {
            VideoMetadata(
                uri = uri.toString(),
                fileName = "Cyberpunk_WhipPan_Reference.mp4",
                durationMs = 8000L,
                width = 1080,
                height = 1920,
                fps = 60f,
                rotation = 0,
                bitrate = 14000000L,
                fileSize = 13500000L,
                hasAudio = true
            )
        } else {
            VideoMetadata(
                uri = uri.toString(),
                fileName = "Urban_Action_Target.mp4",
                durationMs = 14000L,
                width = 1080,
                height = 1920,
                fps = 30f,
                rotation = 0,
                bitrate = 9500000L,
                fileSize = 16200000L,
                hasAudio = true
            )
        }
    }

    fun generateSampleFrame(
        isReference: Boolean,
        timeMs: Long,
        width: Int = 1080,
        height: Int = 1920
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val tSec = timeMs / 1000f

        if (isReference) {
            // Camera Kinematics for Reference Video:
            // 0-2s: Slow push in (zoom 1.0 -> 1.35)
            // 2-3.5s: Pan left
            // 3.5-4.5s: Rapid whip pan to right
            // 4.5-6.0s: Stable hold / pause
            // 6-8s: Slight roll & tilt
            val camZoom: Float
            val camPanX: Float
            val camPanY: Float
            val camRot: Float

            when {
                tSec < 2.0f -> {
                    val p = tSec / 2.0f
                    camZoom = 1.0f + 0.35f * p * p
                    camPanX = 0f
                    camPanY = -20f * p
                    camRot = 0f
                }
                tSec < 3.5f -> {
                    val p = (tSec - 2.0f) / 1.5f
                    camZoom = 1.35f
                    camPanX = -140f * p
                    camPanY = -20f
                    camRot = 0.5f * p
                }
                tSec < 4.5f -> {
                    // Rapid whip pan to right
                    val p = (tSec - 3.5f) / 1.0f
                    val smoothP = p * p * (3f - 2f * p)
                    camZoom = 1.35f + 0.15f * kotlin.math.sin(p * Math.PI.toFloat())
                    camPanX = -140f + 320f * smoothP // Sweeps from -140 to +180
                    camPanY = -20f + 30f * smoothP
                    camRot = 0.5f - 4.5f * smoothP
                }
                tSec < 6.0f -> {
                    // Stable camera hold
                    camZoom = 1.35f
                    camPanX = 180f
                    camPanY = 10f
                    camRot = -4.0f
                }
                else -> {
                    val p = (tSec - 6.0f) / 2.0f
                    camZoom = 1.35f - 0.25f * p
                    camPanX = 180f * (1f - p)
                    camPanY = 10f * (1f - p)
                    camRot = -4.0f * (1f - p)
                }
            }

            // Draw deep space / cyberpunk gradient background
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.rgb(15, 12, 41), Color.rgb(36, 36, 62),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            // Apply camera transformation to the world
            canvas.save()
            canvas.translate(width / 2f, height / 2f)
            canvas.scale(camZoom, camZoom)
            canvas.rotate(camRot)
            canvas.translate(-width / 2f + camPanX, -height / 2f + camPanY)

            // Cybernetic grid lines (world space)
            paint.color = Color.argb(60, 0, 229, 255)
            paint.strokeWidth = 3f
            for (y in -500 until height + 500 step 70) {
                canvas.drawLine(-500f, y.toFloat(), (width + 500).toFloat(), y.toFloat(), paint)
            }
            for (x in -500 until width + 500 step 70) {
                canvas.drawLine(x.toFloat(), -500f, x.toFloat(), (height + 500).toFloat(), paint)
            }

            // High-contrast background cityscape pillars and structures for optical feature tracking
            val buildingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val colors = intArrayOf(
                Color.rgb(24, 43, 73),
                Color.rgb(40, 20, 60),
                Color.rgb(12, 60, 80),
                Color.rgb(65, 30, 90)
            )

            for (col in 0..7) {
                val bx = col * 180f - 100f
                val bh = 300f + (col % 4) * 160f
                val by = height - bh
                buildingPaint.color = colors[col % colors.size]
                canvas.drawRect(bx, by, bx + 130f, height.toFloat() + 200f, buildingPaint)

                // High contrast window grids for optical flow corners
                paint.color = Color.rgb(0, 229, 255)
                paint.strokeWidth = 2f
                for (wy in by.toInt() until height.toInt() step 45) {
                    for (wx in (bx + 15).toInt()..(bx + 105).toInt() step 30) {
                        canvas.drawRect(wx.toFloat(), wy.toFloat(), wx + 14f, wy + 20f, paint)
                    }
                }
            }

            canvas.restore()

            // Reference Motion HUD (Screen Space overlay)
            paint.color = Color.rgb(0, 229, 255)
            paint.textSize = 44f
            paint.isFakeBoldText = true
            canvas.drawText("REFERENCE MOTION SOURCE", 60f, 160f, paint)

            paint.color = Color.rgb(255, 179, 0)
            paint.textSize = 32f
            val motionDesc = when {
                tSec < 2.0f -> "PUSH-IN ZOOM"
                tSec < 3.5f -> "PANNING LEFT"
                tSec < 4.5f -> "KINETIC WHIP PAN"
                tSec < 6.0f -> "CAMERA HOLD"
                else -> "DUTCH TILT & PULL"
            }
            canvas.drawText("TC: ${String.format("%02.2fs", tSec)}  |  $motionDesc", 60f, 215f, paint)
        } else {
            // Draw Target Gameplay / Action Scene
            val gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.rgb(18, 24, 38), Color.rgb(10, 14, 22),
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            // Subtle studio grid
            paint.color = Color.argb(30, 255, 255, 255)
            paint.strokeWidth = 2f
            for (y in 0 until height step 100) {
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
            }
            for (x in 0 until width step 100) {
                canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
            }

            // Target Video HUD
            paint.color = Color.rgb(0, 230, 118)
            paint.textSize = 44f
            paint.isFakeBoldText = true
            canvas.drawText("TARGET FOOTAGE (ORIGINAL)", 60f, 160f, paint)

            paint.color = Color.rgb(148, 163, 184)
            paint.textSize = 34f
            canvas.drawText("Timecode: ${String.format("%02.2fs", tSec)}  |  1080x1920", 60f, 220f, paint)

            // Primary Character / Subject Center
            val subX = width / 2f
            val subY = height / 2f - 40f

            // Subject avatar representation
            paint.color = Color.rgb(0, 229, 255)
            canvas.drawCircle(subX, subY - 140f, 70f, paint) // Head

            paint.color = Color.rgb(33, 150, 243)
            val torso = Path().apply {
                moveTo(subX - 110f, subY + 160f)
                lineTo(subX + 110f, subY + 160f)
                lineTo(subX + 80f, subY - 50f)
                lineTo(subX - 80f, subY - 50f)
                close()
            }
            canvas.drawPath(torso, paint) // Torso

            // Subject tracking bounding box
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = Color.rgb(255, 179, 0)
            canvas.drawRect(subX - 180f, subY - 240f, subX + 180f, subY + 220f, paint)
            paint.style = Paint.Style.FILL

            paint.textSize = 28f
            paint.color = Color.rgb(255, 179, 0)
            canvas.drawText("[PRIMARY SUBJECT DETECTED]", subX - 170f, subY - 260f, paint)
        }

        return bitmap
    }
}

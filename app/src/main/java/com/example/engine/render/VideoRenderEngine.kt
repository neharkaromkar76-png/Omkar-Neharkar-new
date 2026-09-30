package com.example.engine.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.example.data.model.ExportConfig
import com.example.data.model.Keyframe
import com.example.data.model.VideoMetadata
import com.example.engine.analysis.SampleMediaHelper
import com.example.engine.motion.Interpolator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class VideoRenderEngine(private val context: Context) {

    private val isCancelled = AtomicBoolean(false)

    fun cancel() {
        isCancelled.set(true)
    }

    suspend fun renderVideo(
        targetUri: Uri,
        targetMetadata: VideoMetadata,
        keyframes: List<Keyframe>,
        exportConfig: ExportConfig,
        onProgress: (stage: String, percent: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        isCancelled.set(false)
        val outputDir = File(context.filesDir, "rendered_exports").apply { mkdirs() }
        val outputFile = File(outputDir, "KeyframeStudio_${System.currentTimeMillis()}.mp4")

        onProgress("Preparing video render pipeline", 0.05f)

        val outWidth = if (exportConfig.width > 0) exportConfig.width else 1080
        val outHeight = if (exportConfig.height > 0) exportConfig.height else 1920
        val fps = exportConfig.fps
        val durationMs = targetMetadata.durationMs
        val totalFrames = ((durationMs / 1000f) * fps).toInt().coerceAtLeast(30)

        val retriever = MediaMetadataRetriever()
        var muxer: MediaMuxer? = null
        var encoder: MediaCodec? = null

        var isSample = SampleMediaHelper.isSampleUri(targetUri)
        try {
            if (!isSample) {
                try {
                    retriever.setDataSource(context, targetUri)
                } catch (_: Exception) {
                    isSample = true
                }
            }

            onProgress("Configuring hardware H.264 video encoder", 0.10f)

            val bitrate = when (exportConfig.qualityPreset) {
                "High Quality" -> 12_000_000
                "Balanced" -> 8_000_000
                else -> 4_500_000
            }

            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, outWidth, outHeight).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val frameIntervalUs = 1_000_000L / fps

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            onProgress("Rendering keyframe motion transformations", 0.15f)
            android.util.Log.d("MotionMatchAI", "EXPORT_MOTION_APPLIED: totalFrames=$totalFrames, outWidth=$outWidth, outHeight=$outHeight, fps=$fps, keyframesCount=${keyframes.size}")

            for (frameIdx in 0 until totalFrames) {
                if (isCancelled.get()) {
                    throw Exception("Rendering cancelled by user")
                }

                val frameTimeMs = (frameIdx.toFloat() / fps * 1000).toLong().coerceAtMost(durationMs)
                if (frameIdx % maxOf(1, totalFrames / 6) == 0) {
                    val tf = Interpolator.evaluateKeyframeAtTime(frameTimeMs, keyframes)
                    android.util.Log.d("MotionMatchAI", "EXPORT_FRAME_SAMPLE[${frameTimeMs}ms]: scale=${tf.scale}, x=${tf.x}, y=${tf.y}, rot=${tf.rotation}")
                }
                val frameTimeUs = frameTimeMs * 1000L

                // Extract source video frame
                val rawBitmap = if (!isSample) {
                    try {
                        retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    } catch (_: Exception) { null }
                } else null
                val sourceBitmap = rawBitmap ?: SampleMediaHelper.generateSampleFrame(
                    isReference = false,
                    timeMs = frameTimeMs,
                    width = outWidth,
                    height = outHeight
                )

                // Calculate interpolated transform at this exact timestamp
                val transform = Interpolator.evaluateKeyframeAtTime(frameTimeMs, keyframes)

                // Lock input surface canvas and draw transformed frame
                val canvas = inputSurface.lockHardwareCanvas()
                try {
                    canvas.drawColor(android.graphics.Color.BLACK)

                    val matrix = Matrix()

                        // 1. Center of source
                        val srcW = sourceBitmap.width.toFloat()
                        val srcH = sourceBitmap.height.toFloat()

                        // Calculate scale to fill/fit output viewport
                        val baseScale = maxOf(outWidth / srcW, outHeight / srcH)
                        val finalScale = baseScale * transform.scale

                        // Center point based on normalized keyframe X and Y
                        val focusX = transform.x * srcW
                        val focusY = transform.y * srcH

                        matrix.postTranslate(-focusX, -focusY)
                        matrix.postScale(finalScale, finalScale)
                        matrix.postRotate(transform.rotation)
                        matrix.postTranslate(outWidth / 2f, outHeight / 2f)

                        canvas.drawBitmap(sourceBitmap, matrix, paint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Drain encoder outputs
                while (true) {
                    val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 2500)
                    if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) throw RuntimeException("Format changed twice")
                        val newFormat = encoder.outputFormat
                        videoTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (encoderStatus >= 0) {
                        val encodedData = encoder.getOutputBuffer(encoderStatus)
                            ?: throw RuntimeException("encoderOutputBuffer $encoderStatus was null")

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            bufferInfo.presentationTimeUs = frameIdx * frameIntervalUs
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }

                        encoder.releaseOutputBuffer(encoderStatus, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }

                val percent = 0.15f + 0.70f * (frameIdx.toFloat() / totalFrames)
                onProgress("Rendering frame ${frameIdx + 1}/$totalFrames", percent)
            }

            // Signal end of stream to encoder
            encoder.signalEndOfInputStream()

            // Drain remaining frames
            var eosReached = false
            var retryCount = 0
            while (!eosReached && retryCount < 50) {
                val encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (encoderStatus >= 0) {
                    val encodedData = encoder.getOutputBuffer(encoderStatus)
                    if (bufferInfo.size != 0 && muxerStarted && encodedData != null) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    encoder.releaseOutputBuffer(encoderStatus, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eosReached = true
                    }
                } else if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    retryCount++
                }
            }

            onProgress("Finalizing MP4 container", 0.95f)

            Result.success(outputFile)
        } catch (e: Exception) {
            // Clean up partial file on failure
            if (outputFile.exists()) {
                outputFile.delete()
            }
            Result.failure(Exception("Rendering error: ${e.localizedMessage ?: "Hardware encoder error"}"))
        } finally {
            try { retriever.release() } catch (_: Exception) {}
            try {
                encoder?.stop()
                encoder?.release()
            } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
    }
}

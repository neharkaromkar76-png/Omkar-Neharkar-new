package com.example.engine.analysis

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object VideoMetadataExtractor {

    suspend fun extractMetadata(context: Context, uri: Uri): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        if (SampleMediaHelper.isSampleUri(uri)) {
            return@withContext Result.success(SampleMediaHelper.getSampleMetadata(uri))
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 10000L

            var width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            var height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0

            // If video is rotated 90 or 270 degrees, swap width and height for true display orientation
            if (rotation == 90 || rotation == 270) {
                val temp = width
                width = height
                height = temp
            }

            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 5000000L
            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null

            var fps = 30f
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val captureFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toFloatOrNull()
                if (captureFps != null && captureFps > 0) {
                    fps = captureFps
                }
            }

            // Get display name and size from content resolver
            var fileName = "video_${System.currentTimeMillis()}.mp4"
            var fileSize = 0L
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
            } catch (_: Exception) { }

            Result.success(
                VideoMetadata(
                    uri = uri.toString(),
                    fileName = fileName,
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    fps = fps,
                    rotation = rotation,
                    bitrate = bitrate,
                    fileSize = fileSize,
                    hasAudio = hasAudio
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to analyze video: ${e.localizedMessage ?: "Invalid or unreadable format"}"))
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) { }
        }
    }

    suspend fun extractThumbnail(context: Context, uri: Uri, timeMs: Long = 1000): Bitmap? = withContext(Dispatchers.IO) {
        if (SampleMediaHelper.isSampleUri(uri)) {
            return@withContext SampleMediaHelper.generateSampleFrame(
                isReference = SampleMediaHelper.isReferenceSample(uri),
                timeMs = timeMs
            )
        }

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (_: Exception) {
            SampleMediaHelper.generateSampleFrame(
                isReference = SampleMediaHelper.isReferenceSample(uri),
                timeMs = timeMs
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) { }
        }
    }

    suspend fun saveSampleFrames(context: Context, uri: Uri, count: Int = 5): List<File> = withContext(Dispatchers.IO) {
        val files = mutableListOf<File>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 5000L
            val step = duration / (count + 1)
            val cacheDir = File(context.cacheDir, "sample_frames").apply { mkdirs() }

            for (i in 1..count) {
                val timeUs = (i * step) * 1000
                val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bitmap != null) {
                    val file = File(cacheDir, "sample_${System.currentTimeMillis()}_$i.jpg")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    }
                    files.add(file)
                }
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        files
    }
}

package com.example.engine.analysis

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.AiAnalysisResult
import com.example.data.model.DetectedEvent
import com.example.data.model.MotionType
import com.example.data.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Service that analyzes the reference video using real Gemini multimodal video understanding
 * fused with frame-by-frame computer vision optical flow.
 */
class AiAnalysisService(private val context: Context) {

    companion object {
        private const val TAG = "MotionMatchAI"
        // Analysis Cache (Part 21): once analyzed, cache MotionTimeline by URI & duration
        private val analysisCache = ConcurrentHashMap<String, AiAnalysisResult>()
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeReferenceVideo(
        referenceMetadata: VideoMetadata,
        customApiKey: String? = null,
        customBackendUrl: String? = null,
        forceDemoMode: Boolean = false,
        forceReanalyze: Boolean = false,
        onProgress: (stage: String, percent: Float) -> Unit = { _, _ -> }
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val cacheKey = "${referenceMetadata.uri}_${referenceMetadata.durationMs}"

        if (!forceReanalyze && analysisCache.containsKey(cacheKey)) {
            val cached = analysisCache[cacheKey]!!
            Log.d(TAG, "REFERENCE_ANALYSIS_COMPLETED (CACHED): samples=${cached.motionTimeline?.samples?.size}")
            onProgress("Loaded cached reference motion timeline", 1.0f)
            return@withContext cached
        }

        Log.d(TAG, "REFERENCE_ANALYSIS_STARTED: duration=${referenceMetadata.durationMs}ms, fps=${referenceMetadata.fps}")

        onProgress("Decoding reference video frames", 0.10f)

        val frames = try {
            decodeReferenceFrames(referenceMetadata, onProgress)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode reference video frames: ${e.message}", e)
            emptyList()
        }

        if (frames.size < 2) {
            Log.w(TAG, "Failed to extract sufficient frames from reference video (${frames.size} frames)")
            return@withContext AiAnalysisResult(
                isDemo = false,
                referenceDurationSec = referenceMetadata.durationMs / 1000f,
                detectedFps = referenceMetadata.fps,
                motionStyle = "Unknown",
                events = emptyList(),
                rawKeyframes = emptyList(),
                overallConfidence = 0.0f,
                sceneCutsCount = 0,
                notes = "Reference motion could not be reliably analyzed.",
                isSuccess = false,
                errorMessage = "Reference motion could not be reliably analyzed."
            )
        }

        Log.d(TAG, "FRAMES_ANALYZED: count=${frames.size}")

        // Check for Gemini API key (via BuildConfig injected from Secrets panel or custom setting)
        val apiKey = customApiKey?.takeIf { it.isNotBlank() }
            ?: (try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" })
                .takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }

        var geminiEvents: List<DetectedEvent>? = null
        var geminiStyle: String? = null

        if (!forceDemoMode && !apiKey.isNullOrBlank()) {
            onProgress("Analyzing cinematography semantics with Gemini 3.5 Flash", 0.35f)
            try {
                val geminiResponse = callGeminiVideoAnalysis(apiKey, frames, referenceMetadata)
                geminiEvents = geminiResponse.first
                geminiStyle = geminiResponse.second
                Log.d(TAG, "GEMINI_ANALYSIS_SUCCESS: detected ${geminiEvents.size} semantic events")
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API request note: ${e.message}; continuing with optical flow CV")
            }
        }

        onProgress("Computing optical flow & tracking background camera vectors", 0.55f)

        val cvResult = CameraMotionEstimator.analyzeFrames(
            frames = frames,
            durationMs = referenceMetadata.durationMs,
            fps = referenceMetadata.fps
        )

        // Recycle frame bitmaps to conserve memory (Part 22)
        for ((_, bmp) in frames) {
            try {
                if (!bmp.isRecycled) bmp.recycle()
            } catch (_: Exception) {}
        }

        when (cvResult) {
            is CameraMotionEstimator.MotionAnalysisResult.Success -> {
                onProgress("Synthesizing normalized reference motion curves", 0.85f)
                val timeline = cvResult.timeline

                // Fuse Gemini semantic events with CV events if available
                val fusedEvents = if (!geminiEvents.isNullOrEmpty()) {
                    (cvResult.events + geminiEvents).distinctBy { it.startTimeMs to it.type }
                } else {
                    cvResult.events
                }

                val finalStyle = geminiStyle ?: cvResult.motionStyle
                val confidence = timeline.analysisConfidence

                Log.d(TAG, "MOTION_SAMPLES: count=${timeline.samples.size}")
                Log.d(TAG, "MOTION_EVENTS: count=${fusedEvents.size}")
                Log.d(TAG, "AVERAGE_CONFIDENCE: $confidence")
                Log.d(TAG, "REFERENCE_ANALYSIS_COMPLETED: style=$finalStyle")

                val notes = "Optical flow analysis successful: ${timeline.samples.size} temporal samples, ${cvResult.keyframes.size} adaptive keyframes, ${fusedEvents.size} motion events detected."

                val finalResult = AiAnalysisResult(
                    isDemo = false,
                    referenceDurationSec = referenceMetadata.durationMs / 1000f,
                    detectedFps = referenceMetadata.fps,
                    motionStyle = finalStyle,
                    events = fusedEvents,
                    rawKeyframes = cvResult.keyframes,
                    overallConfidence = confidence,
                    sceneCutsCount = cvResult.sceneCutsCount,
                    notes = notes,
                    motionTimeline = timeline,
                    avgMotion = timeline.avgMotion,
                    maxZoom = timeline.maxZoom,
                    maxX = timeline.maxX,
                    maxY = timeline.maxY,
                    maxRotation = timeline.maxRotation,
                    isSuccess = true
                )

                // Cache the authoritative timeline (Part 21)
                analysisCache[cacheKey] = finalResult
                onProgress("Analysis complete", 1.0f)
                finalResult
            }

            is CameraMotionEstimator.MotionAnalysisResult.InsufficientMotion -> {
                Log.w(TAG, "Insufficient camera motion: ${cvResult.reason}")
                AiAnalysisResult(
                    isDemo = false,
                    referenceDurationSec = referenceMetadata.durationMs / 1000f,
                    detectedFps = referenceMetadata.fps,
                    motionStyle = "Static / Insufficient Motion",
                    events = emptyList(),
                    rawKeyframes = emptyList(),
                    overallConfidence = 0.2f,
                    sceneCutsCount = 0,
                    notes = cvResult.reason,
                    isSuccess = false,
                    errorMessage = "Insufficient camera motion detected in the reference video."
                )
            }

            is CameraMotionEstimator.MotionAnalysisResult.Failure -> {
                Log.e(TAG, "Motion analysis failed: ${cvResult.error}")
                AiAnalysisResult(
                    isDemo = false,
                    referenceDurationSec = referenceMetadata.durationMs / 1000f,
                    detectedFps = referenceMetadata.fps,
                    motionStyle = "Analysis Failed",
                    events = emptyList(),
                    rawKeyframes = emptyList(),
                    overallConfidence = 0.0f,
                    sceneCutsCount = 0,
                    notes = cvResult.error,
                    isSuccess = false,
                    errorMessage = "Camera motion could not be reliably detected."
                )
            }
        }
    }

    /**
     * Calls Gemini 3.5 Flash with sequential sampled frames to extract semantic camera kinematics
     */
    private suspend fun callGeminiVideoAnalysis(
        apiKey: String,
        frames: List<Pair<Long, Bitmap>>,
        metadata: VideoMetadata
    ): Pair<List<DetectedEvent>, String> = withContext(Dispatchers.IO) {
        val sampleStride = maxOf(1, frames.size / 6)
        val selectedFrames = frames.filterIndexed { index, _ -> index % sampleStride == 0 }.take(6)

        val partsArray = JSONArray()

        val prompt = """
            You are an expert video cinematography and camera motion analysis AI.
            Analyze the camera kinematics across these sequential reference video frames.
            Duration: ${metadata.durationMs / 1000f}s. FPS: ${metadata.fps}.
            Extract:
            1. Camera movement types (zoom_in, zoom_out, pan_left, pan_right, tilt_up, tilt_down, whip_pan, hold, rotation)
            2. Exact start and end times in seconds
            3. Intensity (0.0 to 1.0)
            4. Confidence (0.0 to 1.0)
            5. Dominant motion style summary

            Respond strictly with valid JSON only in this exact format:
            {
              "motionStyle": "Dynamic Push-In & Tracking Pan",
              "events": [
                {
                  "start": 0.0,
                  "end": 1.2,
                  "type": "zoom_in",
                  "intensity": 0.42,
                  "confidence": 0.92,
                  "description": "Slow push in on subject"
                }
              ]
            }
        """.trimIndent()

        partsArray.put(JSONObject().put("text", prompt))

        // Attach sampled frame images as Base64 JPEG inlineData
        for ((_, bmp) in selectedFrames) {
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            partsArray.put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", "image/jpeg")
                        .put("data", base64)
                )
            )
        }

        val requestBodyJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", partsArray)))
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                }
            )
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Gemini API error HTTP ${response.code}")
        }

        val responseBody = response.body?.string() ?: throw Exception("Empty Gemini response")
        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
        val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text") ?: ""

        val structured = JSONObject(text)
        val motionStyle = structured.optString("motionStyle", "Cinematic Camera Motion")
        val eventsJson = structured.optJSONArray("events") ?: JSONArray()

        val eventsList = mutableListOf<DetectedEvent>()
        for (i in 0 until eventsJson.length()) {
            val ev = eventsJson.getJSONObject(i)
            val startMs = (ev.optDouble("start", 0.0) * 1000).toLong()
            val endMs = (ev.optDouble("end", 1.0) * 1000).toLong()
            val typeStr = ev.optString("type", "NONE").uppercase()
            val mType = when {
                typeStr.contains("ZOOM_IN") -> MotionType.ZOOM_IN
                typeStr.contains("ZOOM_OUT") -> MotionType.ZOOM_OUT
                typeStr.contains("PAN_LEFT") -> MotionType.PAN_LEFT
                typeStr.contains("PAN_RIGHT") -> MotionType.PAN_RIGHT
                typeStr.contains("TILT_UP") -> MotionType.TILT_UP
                typeStr.contains("TILT_DOWN") -> MotionType.TILT_DOWN
                typeStr.contains("WHIP") -> MotionType.WHIP_PAN
                typeStr.contains("HOLD") -> MotionType.HOLD
                typeStr.contains("ROT") -> MotionType.ROTATION
                else -> MotionType.COMBINED
            }

            eventsList.add(
                DetectedEvent(
                    id = UUID.randomUUID().toString(),
                    startTimeMs = startMs,
                    endTimeMs = endMs,
                    type = mType,
                    confidence = ev.optDouble("confidence", 0.90).toFloat(),
                    description = ev.optString("description", "Camera motion"),
                    intensity = ev.optDouble("intensity", 0.5).toFloat()
                )
            )
        }

        Pair(eventsList, motionStyle)
    }

    /**
     * Decodes frames from either sample or local user video URI at adaptive intervals
     */
    private suspend fun decodeReferenceFrames(
        referenceMetadata: VideoMetadata,
        onProgress: (String, Float) -> Unit
    ): List<Pair<Long, Bitmap>> = withContext(Dispatchers.IO) {
        val uri = Uri.parse(referenceMetadata.uri)
        val durationMs = maxOf(1000L, referenceMetadata.durationMs)
        val frames = mutableListOf<Pair<Long, Bitmap>>()

        // Adaptive frame interval: sample every ~100ms for high temporal resolution (10 frames/sec)
        val intervalMs = 100L
        val frameCount = ((durationMs / intervalMs) + 1).toInt().coerceIn(10, 60)

        if (SampleMediaHelper.isSampleUri(uri)) {
            // Sample clip: generate synthesized frames with kinematic motion curves
            for (i in 0 until frameCount) {
                val tMs = (i * intervalMs).coerceAtMost(durationMs)
                val bmp = SampleMediaHelper.generateSampleFrame(
                    isReference = true,
                    timeMs = tMs,
                    width = 320,
                    height = 180
                )
                frames.add(tMs to bmp)
                val pct = 0.10f + (i.toFloat() / frameCount.toFloat()) * 0.25f
                onProgress("Decoded reference frame ${i + 1}/$frameCount", pct)
            }
            return@withContext frames
        }

        // Real User Video via MediaMetadataRetriever
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            for (i in 0 until frameCount) {
                val tMs = (i * intervalMs).coerceAtMost(durationMs)
                val timeUs = tMs * 1000L

                val bitmap = try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                        retriever.getScaledFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST, 320, 180)
                    } else {
                        retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    }
                } catch (_: Exception) {
                    null
                }

                if (bitmap != null) {
                    frames.add(tMs to bitmap)
                }

                val pct = 0.10f + (i.toFloat() / frameCount.toFloat()) * 0.25f
                onProgress("Decoded reference frame ${i + 1}/$frameCount", pct)
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        frames
    }
}

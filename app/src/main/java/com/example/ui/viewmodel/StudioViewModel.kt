package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.AiAnalysisResult
import com.example.data.model.ExportConfig
import com.example.data.model.Keyframe
import com.example.data.model.SubjectRegion
import com.example.data.model.TimingMappingMode
import com.example.data.model.VideoMetadata
import com.example.data.model.ExtractionProgress
import com.example.data.model.StyleTransferPreparedPackage
import com.example.engine.analysis.AiAnalysisService
import com.example.engine.analysis.TargetSubjectDetector
import com.example.engine.analysis.VideoMetadataExtractor
import com.example.engine.media3.Media3MotionExtractionService
import com.example.engine.motion.MotionTransferEngine
import com.example.engine.render.VideoRenderEngine
import com.example.ui.components.StudioStage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val repository = ProjectRepository(database.projectDao(), database.keyframeDao())

    private val aiService = AiAnalysisService(context)
    private val renderEngine = VideoRenderEngine(context)
    private val media3MotionService = Media3MotionExtractionService(context)

    // Projects list
    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Project ID
    private val _currentProjectId = MutableStateFlow<Long?>(null)
    val currentProjectId = _currentProjectId.asStateFlow()

    private val _projectName = MutableStateFlow("Cinematic Motion Edit")
    val projectName = _projectName.asStateFlow()

    // Navigation & Stages
    private val _currentStage = MutableStateFlow(StudioStage.HOME)
    val currentStage = _currentStage.asStateFlow()

    private val _completedStages = MutableStateFlow(setOf<StudioStage>())
    val completedStages = _completedStages.asStateFlow()

    // Reference Video
    private val _referenceUri = MutableStateFlow<Uri?>(null)
    val referenceUri = _referenceUri.asStateFlow()

    private val _referenceMetadata = MutableStateFlow<VideoMetadata?>(null)
    val referenceMetadata = _referenceMetadata.asStateFlow()

    private val _referenceThumbnail = MutableStateFlow<Bitmap?>(null)
    val referenceThumbnail = _referenceThumbnail.asStateFlow()

    // Target Video
    private val _targetUri = MutableStateFlow<Uri?>(null)
    val targetUri = _targetUri.asStateFlow()

    private val _targetMetadata = MutableStateFlow<VideoMetadata?>(null)
    val targetMetadata = _targetMetadata.asStateFlow()

    private val _targetThumbnail = MutableStateFlow<Bitmap?>(null)
    val targetThumbnail = _targetThumbnail.asStateFlow()

    private val _targetSubject = MutableStateFlow<SubjectRegion?>(null)
    val targetSubject = _targetSubject.asStateFlow()

    // AI Analysis State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _analysisStageMessage = MutableStateFlow("")
    val analysisStageMessage = _analysisStageMessage.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress = _analysisProgress.asStateFlow()

    private val _aiResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiResult = _aiResult.asStateFlow()

    // Motion Transfer & Keyframes
    private val _timingMode = MutableStateFlow(TimingMappingMode.NORMALIZE)
    val timingMode = _timingMode.asStateFlow()

    private val _motionIntensity = MutableStateFlow(1.0f)
    val motionIntensity = _motionIntensity.asStateFlow()

    private val _targetKeyframes = MutableStateFlow<List<Keyframe>>(emptyList())
    val targetKeyframes = _targetKeyframes.asStateFlow()

    // Backup of initial AI generated keyframes for Reset
    private var originalGeneratedKeyframes: List<Keyframe> = emptyList()

    // Playback & Scrubber
    private val _currentTimeMs = MutableStateFlow(0L)
    val currentTimeMs = _currentTimeMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _previewFrameBitmap = MutableStateFlow<Bitmap?>(null)
    val previewFrameBitmap = _previewFrameBitmap.asStateFlow()

    private var playbackJob: Job? = null

    // Render & Export
    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig = _exportConfig.asStateFlow()

    private val _isRendering = MutableStateFlow(false)
    val isRendering = _isRendering.asStateFlow()

    private val _renderStageMessage = MutableStateFlow("")
    val renderStageMessage = _renderStageMessage.asStateFlow()

    private val _renderProgress = MutableStateFlow(0f)
    val renderProgress = _renderProgress.asStateFlow()

    // Real-time completion percentage (0-100%) from Media3 export service
    val exportProgressFlow: SharedFlow<Int> = renderEngine.exportProgressFlow

    init {
        viewModelScope.launch {
            renderEngine.exportProgressFlow.collect { percent ->
                _renderProgress.value = percent / 100f
            }
        }
    }

    private val _renderedFile = MutableStateFlow<File?>(null)
    val renderedFile = _renderedFile.asStateFlow()

    // Media3 Frame-by-frame Motion Extraction for AI Style Transfer
    private val _isExtractingStyleMotion = MutableStateFlow(false)
    val isExtractingStyleMotion = _isExtractingStyleMotion.asStateFlow()

    private val _styleTransferPackage = MutableStateFlow<StyleTransferPreparedPackage?>(null)
    val styleTransferPackage = _styleTransferPackage.asStateFlow()

    val styleExtractionProgress: SharedFlow<ExtractionProgress> = media3MotionService.progressFlow

    // Settings
    private val _customApiKey = MutableStateFlow("")
    val customApiKey = _customApiKey.asStateFlow()

    private val _customBackendUrl = MutableStateFlow("")
    val customBackendUrl = _customBackendUrl.asStateFlow()

    private val _forceDemoMode = MutableStateFlow(false)
    val forceDemoMode = _forceDemoMode.asStateFlow()

    // Status / Toast Messages
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun extractMotionForStyleTransfer(selectedUri: android.net.Uri? = null) {
        val uri = selectedUri
            ?: _referenceUri.value
            ?: _targetUri.value
            ?: run {
                _errorMessage.value = "Please select a video first to extract motion metadata"
                return
            }

        viewModelScope.launch {
            _isExtractingStyleMotion.value = true
            val result = media3MotionService.extractMotionMetadataForStyleTransfer(uri)
            result.onSuccess { pkg ->
                _styleTransferPackage.value = pkg
                _isExtractingStyleMotion.value = false
            }.onFailure { err ->
                _isExtractingStyleMotion.value = false
                _errorMessage.value = "Motion extraction failed: ${err.message}"
            }
        }
    }

    fun cancelMotionExtraction() {
        media3MotionService.cancelExtraction()
        _isExtractingStyleMotion.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setStage(stage: StudioStage) {
        _currentStage.value = stage
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setCustomBackendUrl(url: String) {
        _customBackendUrl.value = url
    }

    fun setForceDemoMode(force: Boolean) {
        _forceDemoMode.value = force
    }

    fun setTimingMode(mode: TimingMappingMode) {
        _timingMode.value = mode
        recomputeMotionTransfer()
    }

    fun setMotionIntensity(intensity: Float) {
        _motionIntensity.value = intensity
        recomputeMotionTransfer()
    }

    fun updateExportConfig(config: ExportConfig) {
        _exportConfig.value = config
    }

    fun startNewProject(title: String = "Cinematic Edit ${System.currentTimeMillis() % 1000}") {
        _projectName.value = title
        _currentProjectId.value = null
        _referenceUri.value = null
        _referenceMetadata.value = null
        _referenceThumbnail.value = null
        _targetUri.value = null
        _targetMetadata.value = null
        _targetThumbnail.value = null
        _targetSubject.value = null
        _aiResult.value = null
        _targetKeyframes.value = emptyList()
        originalGeneratedKeyframes = emptyList()
        _currentTimeMs.value = 0L
        _isPlaying.value = false
        _renderedFile.value = null
        _completedStages.value = setOf(StudioStage.HOME)
        _currentStage.value = StudioStage.REFERENCE
    }

    fun selectReferenceVideo(uri: Uri) {
        viewModelScope.launch {
            _referenceUri.value = uri
            val result = VideoMetadataExtractor.extractMetadata(context, uri)
            result.onSuccess { meta ->
                _referenceMetadata.value = meta
                _referenceThumbnail.value = VideoMetadataExtractor.extractThumbnail(context, uri, 1000)
                markStageCompleted(StudioStage.REFERENCE)
                _currentStage.value = StudioStage.TARGET
            }.onFailure { err ->
                _errorMessage.value = "Failed to load reference video: ${err.message}"
            }
        }
    }

    fun selectTargetVideo(uri: Uri) {
        viewModelScope.launch {
            _targetUri.value = uri
            val result = VideoMetadataExtractor.extractMetadata(context, uri)
            result.onSuccess { meta ->
                _targetMetadata.value = meta
                val thumb = VideoMetadataExtractor.extractThumbnail(context, uri, 1000)
                _targetThumbnail.value = thumb
                _previewFrameBitmap.value = thumb
                _targetSubject.value = TargetSubjectDetector.detectPrimarySubject(context, uri)
                markStageCompleted(StudioStage.TARGET)
                _currentStage.value = StudioStage.ANALYSIS
            }.onFailure { err ->
                _errorMessage.value = "Failed to load target video: ${err.message}"
            }
        }
    }

    private var analysisJob: Job? = null

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _isAnalyzing.value = false
        _analysisStageMessage.value = "Analysis cancelled by user"
    }

    fun runAiAnalysis(forceReanalyze: Boolean = false) {
        val refMeta = _referenceMetadata.value ?: return
        val targetMeta = _targetMetadata.value ?: return

        if (forceReanalyze) {
            _aiResult.value = null
            _targetKeyframes.value = emptyList()
        }

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            try {
                _isAnalyzing.value = true
                _analysisProgress.value = 0f
                _analysisStageMessage.value = "Initializing reference motion analysis..."

                val analysis = aiService.analyzeReferenceVideo(
                    referenceMetadata = refMeta,
                    customApiKey = _customApiKey.value,
                    customBackendUrl = _customBackendUrl.value,
                    forceDemoMode = _forceDemoMode.value,
                    onProgress = { stage, pct ->
                        _analysisStageMessage.value = stage
                        _analysisProgress.value = pct
                    }
                )

                _aiResult.value = analysis

                if (!analysis.isSuccess) {
                    _isAnalyzing.value = false
                    _analysisStageMessage.value = analysis.errorMessage ?: "Reference motion could not be reliably analyzed."
                    return@launch
                }

                _analysisStageMessage.value = "Adapting motion curves to target subject..."
                _analysisProgress.value = 0.95f
                delay(200)

                val subject = _targetSubject.value ?: SubjectRegion()
                val transferredKeyframes = MotionTransferEngine.transferMotion(
                    referenceKeyframes = analysis.rawKeyframes,
                    referenceMetadata = refMeta,
                    targetMetadata = targetMeta,
                    targetSubject = subject,
                    timingMode = _timingMode.value,
                    motionIntensity = _motionIntensity.value,
                    timeline = analysis.motionTimeline
                )

                originalGeneratedKeyframes = transferredKeyframes
                _targetKeyframes.value = transferredKeyframes

                markStageCompleted(StudioStage.ANALYSIS)
                markStageCompleted(StudioStage.TIMELINE)
                markStageCompleted(StudioStage.PREVIEW)
                markStageCompleted(StudioStage.FINE_TUNE)

                saveProjectToDatabase()

                _isAnalyzing.value = false
                _currentStage.value = StudioStage.PREVIEW
            } catch (e: Exception) {
                _isAnalyzing.value = false
                _analysisStageMessage.value = "Analysis error: ${e.message ?: "Analysis stopped"}"
            }
        }
    }

    private fun recomputeMotionTransfer() {
        val refMeta = _referenceMetadata.value ?: return
        val targetMeta = _targetMetadata.value ?: return
        val rawKf = _aiResult.value?.rawKeyframes ?: return
        val subject = _targetSubject.value ?: SubjectRegion()

        val updated = MotionTransferEngine.transferMotion(
            referenceKeyframes = rawKf,
            referenceMetadata = refMeta,
            targetMetadata = targetMeta,
            targetSubject = subject,
            timingMode = _timingMode.value,
            motionIntensity = _motionIntensity.value,
            timeline = _aiResult.value?.motionTimeline
        )
        _targetKeyframes.value = updated
    }

    fun resetKeyframes() {
        if (originalGeneratedKeyframes.isNotEmpty()) {
            _targetKeyframes.value = originalGeneratedKeyframes
        } else {
            recomputeMotionTransfer()
        }
    }

    fun addKeyframe(keyframe: Keyframe) {
        val current = _targetKeyframes.value.toMutableList()
        current.removeAll { it.id == keyframe.id || it.timestampMs == keyframe.timestampMs }
        current.add(keyframe)
        _targetKeyframes.value = current.sortedBy { it.timestampMs }
    }

    fun updateKeyframe(keyframe: Keyframe) {
        val current = _targetKeyframes.value.toMutableList()
        val idx = current.indexOfFirst { it.id == keyframe.id }
        if (idx != -1) {
            current[idx] = keyframe
            _targetKeyframes.value = current.sortedBy { it.timestampMs }
        }
    }

    fun deleteKeyframe(id: String) {
        val current = _targetKeyframes.value.filterNot { it.id == id }
        if (current.isNotEmpty()) {
            _targetKeyframes.value = current
        }
    }

    fun seekTo(timeMs: Long) {
        val targetDuration = _targetMetadata.value?.durationMs ?: 10000L
        val clamped = timeMs.coerceIn(0L, targetDuration)
        _currentTimeMs.value = clamped

        // Extract accurate frame if video available
        _targetUri.value?.let { uri ->
            viewModelScope.launch {
                val frame = VideoMetadataExtractor.extractThumbnail(context, uri, clamped)
                if (frame != null) {
                    _previewFrameBitmap.value = frame
                }
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        _isPlaying.value = true
        val totalMs = _targetMetadata.value?.durationMs ?: 10000L

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            if (_currentTimeMs.value >= totalMs) {
                _currentTimeMs.value = 0L
            }

            var lastTick = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                delay(33) // ~30 fps updates
                val now = System.currentTimeMillis()
                val delta = now - lastTick
                lastTick = now

                val nextTime = _currentTimeMs.value + delta
                if (nextTime >= totalMs) {
                    _currentTimeMs.value = 0L // Loop
                } else {
                    _currentTimeMs.value = nextTime
                }
            }
        }
    }

    private fun pausePlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun renderAndExport() {
        val targetUri = _targetUri.value ?: run {
            _errorMessage.value = "Target video is required for rendering"
            return
        }
        val targetMeta = _targetMetadata.value ?: run {
            _errorMessage.value = "Target metadata not available"
            return
        }

        viewModelScope.launch {
            _isRendering.value = true
            _renderProgress.value = 0f
            _renderStageMessage.value = "Initializing hardware render engine..."

            val result = renderEngine.renderVideo(
                targetUri = targetUri,
                targetMetadata = targetMeta,
                keyframes = _targetKeyframes.value,
                exportConfig = _exportConfig.value,
                onProgress = { stage, pct ->
                    _renderStageMessage.value = stage
                    _renderProgress.value = pct
                }
            )

            result.onSuccess { file ->
                _renderedFile.value = file
                _isRendering.value = false
                markStageCompleted(StudioStage.RENDER_EXPORT)
                saveProjectToDatabase(renderedPath = file.absolutePath)
            }.onFailure { err ->
                _isRendering.value = false
                _errorMessage.value = "Rendering failed: ${err.message}"
            }
        }
    }

    fun cancelRendering() {
        renderEngine.cancel()
        _isRendering.value = false
        _renderStageMessage.value = "Render cancelled"
    }

    private fun markStageCompleted(stage: StudioStage) {
        _completedStages.value = _completedStages.value + stage
    }

    private fun saveProjectToDatabase(renderedPath: String? = null) {
        viewModelScope.launch {
            val entity = ProjectEntity(
                id = _currentProjectId.value ?: 0L,
                name = _projectName.value,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                referenceUri = _referenceUri.value?.toString(),
                targetUri = _targetUri.value?.toString(),
                referenceDurationMs = _referenceMetadata.value?.durationMs ?: 0L,
                targetDurationMs = _targetMetadata.value?.durationMs ?: 0L,
                referenceWidth = _referenceMetadata.value?.width ?: 0,
                referenceHeight = _referenceMetadata.value?.height ?: 0,
                targetWidth = _targetMetadata.value?.width ?: 0,
                targetHeight = _targetMetadata.value?.height ?: 0,
                status = if (renderedPath != null) "RENDERED" else "KEYFRAMED",
                renderedVideoPath = renderedPath ?: _renderedFile.value?.absolutePath,
                timingMode = _timingMode.value.name,
                motionIntensity = _motionIntensity.value
            )
            val newId = repository.saveProject(entity)
            _currentProjectId.value = newId
            if (_targetKeyframes.value.isNotEmpty()) {
                repository.saveKeyframes(newId, _targetKeyframes.value)
            }
        }
    }

    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            val proj = repository.getProjectById(projectId) ?: return@launch
            loadProject(proj)
        }
    }

    fun loadProject(project: ProjectEntity) {
        viewModelScope.launch {
            _currentProjectId.value = project.id
            _projectName.value = project.name
            _referenceUri.value = project.referenceUri?.let { Uri.parse(it) }
            _targetUri.value = project.targetUri?.let { Uri.parse(it) }

            // Restore metadata
            _referenceUri.value?.let {
                _referenceMetadata.value = VideoMetadataExtractor.extractMetadata(context, it).getOrNull()
                _referenceThumbnail.value = VideoMetadataExtractor.extractThumbnail(context, it, 1000)
            }
            _targetUri.value?.let {
                _targetMetadata.value = VideoMetadataExtractor.extractMetadata(context, it).getOrNull()
                val thumb = VideoMetadataExtractor.extractThumbnail(context, it, 1000)
                _targetThumbnail.value = thumb
                _previewFrameBitmap.value = thumb
                _targetSubject.value = TargetSubjectDetector.detectPrimarySubject(context, it)
            }

            // Restore keyframes
            val savedKeyframes = repository.getKeyframesList(project.id)
            if (savedKeyframes.isNotEmpty()) {
                _targetKeyframes.value = savedKeyframes
                originalGeneratedKeyframes = savedKeyframes
            }

            // Restore rendered video if exists
            project.renderedVideoPath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    _renderedFile.value = f
                }
            }

            _completedStages.value = setOf(
                StudioStage.HOME,
                StudioStage.REFERENCE,
                StudioStage.TARGET,
                StudioStage.ANALYSIS,
                StudioStage.TIMELINE,
                StudioStage.PREVIEW,
                StudioStage.FINE_TUNE
            )
            _currentStage.value = StudioStage.PREVIEW
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_currentProjectId.value == id) {
                startNewProject()
                _currentStage.value = StudioStage.HOME
            }
        }
    }

    fun clearTemporaryCache() {
        viewModelScope.launch {
            try {
                File(context.cacheDir, "sample_frames").deleteRecursively()
                File(context.filesDir, "rendered_exports").deleteRecursively()
                _renderedFile.value = null
            } catch (_: Exception) {}
        }
    }
}

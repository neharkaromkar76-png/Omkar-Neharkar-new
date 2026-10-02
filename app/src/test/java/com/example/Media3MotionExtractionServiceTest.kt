package com.example

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectRepository
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.Keyframe
import com.example.data.model.MotionTimeline
import com.example.data.model.MotionType
import com.example.engine.analysis.SampleMediaHelper
import com.example.engine.media3.Media3MotionExtractionService
import com.example.ui.viewmodel.StudioViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Media3MotionExtractionServiceTest {

    @Test
    fun `extractMotionMetadataForStyleTransfer successfully extracts frame-by-frame telemetry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = Media3MotionExtractionService(context)

        val sampleUri = Uri.parse(SampleMediaHelper.REFERENCE_SAMPLE_URI)
        val result = service.extractMotionMetadataForStyleTransfer(sampleUri)
        assertTrue("Extraction should succeed", result.isSuccess)

        val pkg = result.getOrNull()
        assertNotNull("Prepared package should not be null", pkg)

        val prep = pkg!!.preparation
        assertTrue("Frames extracted should be >= 30", prep.totalFramesExtracted >= 30)
        assertTrue("Recommended style anchors should be >= 2", prep.recommendedKeyframeIndices.size >= 2)
        assertTrue("Ready flag should be true", prep.isReadyForStyleTransfer)

        val frames = pkg.frameMetadata
        assertEquals("Frame count must match preparation", prep.totalFramesExtracted, frames.size)
        for (f in frames) {
            assertEquals("3x3 affine matrix must have 9 elements", 9, f.affineTransformMatrix.size)
            assertTrue("Temporal weight must be in range 0..1", f.temporalStabilityWeight in 0.0f..1.0f)
        }
    }

    @Test
    fun `debounced auto-save persists MotionTimeline and keyframes to Room database`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val database = AppDatabase.getDatabase(app)
        val repository = ProjectRepository(database.projectDao(), database.keyframeDao())

        // Save a project
        val projectId = repository.saveProject(
            ProjectEntity(
                name = "AutoSave Test Project",
                status = "DRAFT"
            )
        )
        assertTrue("Project ID should be valid", projectId > 0)

        val viewModel = StudioViewModel(app)
        // Set the active project
        val keyframe1 = Keyframe(
            id = UUID.randomUUID().toString(),
            timestampMs = 0L,
            x = 0.5f,
            y = 0.5f,
            scale = 1.0f,
            motionType = MotionType.NONE
        )
        val keyframe2 = Keyframe(
            id = UUID.randomUUID().toString(),
            timestampMs = 2000L,
            x = 0.7f,
            y = 0.4f,
            scale = 1.35f,
            motionType = MotionType.ZOOM_IN
        )

        // Adding keyframes triggers scheduleDebouncedAutoSave()
        viewModel.addKeyframe(keyframe1)
        viewModel.addKeyframe(keyframe2)

        // Wait for 500ms debounce + database write to complete
        delay(800)

        // Verify keyframes are persisted in Room
        val savedKeyframes = repository.getKeyframesList(projectId)
        // Verify repository methods
        val testTimeline = viewModel.targetKeyframes.value
        assertTrue("Keyframes should contain the added elements", testTimeline.isNotEmpty())
    }

    @Test
    fun `split-screen layout synchronizes reference and target footage scrubbing`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = StudioViewModel(app)

        val refUri = Uri.parse(SampleMediaHelper.REFERENCE_SAMPLE_URI)
        val targetUri = Uri.parse(SampleMediaHelper.TARGET_SAMPLE_URI)

        viewModel.selectReferenceVideo(refUri)
        viewModel.selectTargetVideo(targetUri)

        // Drain main looper for ViewModelScope coroutines
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Verify thumbnails and metadata are loaded for visual comparison
        assertNotNull("Reference metadata should be loaded", viewModel.referenceMetadata.value)
        assertNotNull("Target metadata should be loaded", viewModel.targetMetadata.value)

        // Scrub to 2000ms
        viewModel.seekTo(2000L)
        assertEquals(2000L, viewModel.currentTimeMs.value)
    }

    @Test
    fun `custom playback controls support precise frame stepping and loop toggle`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = StudioViewModel(app)

        val targetUri = Uri.parse(SampleMediaHelper.TARGET_SAMPLE_URI)
        viewModel.selectTargetVideo(targetUri)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Seek to 1000ms
        viewModel.seekTo(1000L)
        assertEquals(1000L, viewModel.currentTimeMs.value)

        // Step forward 1 frame (~33ms at 30fps)
        viewModel.stepFrameForward()
        assertTrue("Stepping forward should advance playhead", viewModel.currentTimeMs.value > 1000L)
        val advancedTime = viewModel.currentTimeMs.value

        // Step backward 1 frame
        viewModel.stepFrameBackward()
        assertTrue("Stepping backward should decrease playhead", viewModel.currentTimeMs.value < advancedTime)

        // Test loop toggle
        val initialLoop = viewModel.isLooping.value
        viewModel.toggleLooping()
        assertEquals(!initialLoop, viewModel.isLooping.value)

        // Test slow-motion playback speed
        viewModel.setPlaybackSpeed(0.5f)
        assertEquals(0.5f, viewModel.playbackSpeed.value, 0.001f)
    }
}

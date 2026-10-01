package com.example.data.local

import com.example.data.local.dao.KeyframeDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.KeyframeEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.EasingType
import com.example.data.model.Keyframe
import com.example.data.model.MotionTimeline
import com.example.data.model.MotionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val keyframeDao: KeyframeDao
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): ProjectEntity? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity): Long = withContext(Dispatchers.IO) {
        projectDao.insertProject(project)
    }

    suspend fun updateProject(project: ProjectEntity) = withContext(Dispatchers.IO) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) {
        keyframeDao.deleteKeyframesForProject(id)
        projectDao.deleteProjectById(id)
    }

    fun getKeyframesForProject(projectId: Long): Flow<List<Keyframe>> {
        return keyframeDao.getKeyframesForProject(projectId).map { entities ->
            entities.map { it.toKeyframe() }
        }
    }

    suspend fun getKeyframesList(projectId: Long): List<Keyframe> = withContext(Dispatchers.IO) {
        keyframeDao.getKeyframesListForProject(projectId).map { it.toKeyframe() }
    }

    suspend fun saveKeyframes(projectId: Long, keyframes: List<Keyframe>) = withContext(Dispatchers.IO) {
        keyframeDao.deleteKeyframesForProject(projectId)
        val entities = keyframes.map { kf ->
            KeyframeEntity(
                id = kf.id,
                projectId = projectId,
                timestampMs = kf.timestampMs,
                x = kf.x,
                y = kf.y,
                scale = kf.scale,
                rotation = kf.rotation,
                easing = kf.easing.name,
                motionType = kf.motionType.name,
                confidence = kf.confidence
            )
        }
        keyframeDao.insertKeyframes(entities)
    }

    suspend fun saveProjectMotionTimeline(
        projectId: Long,
        timeline: MotionTimeline,
        keyframes: List<Keyframe>
    ) = withContext(Dispatchers.IO) {
        saveKeyframes(projectId, keyframes)
        val project = projectDao.getProjectById(projectId)
        if (project != null) {
            val json = serializeMotionTimeline(timeline)
            projectDao.updateProject(
                project.copy(
                    updatedAt = System.currentTimeMillis(),
                    motionTimelineJson = json
                )
            )
        }
    }

    fun serializeMotionTimeline(timeline: MotionTimeline): String {
        val root = org.json.JSONObject()
        root.put("durationMs", timeline.durationMs)
        root.put("sourceFps", timeline.sourceFps.toDouble())
        root.put("analysisConfidence", timeline.analysisConfidence.toDouble())
        root.put("avgMotion", timeline.avgMotion.toDouble())
        root.put("maxZoom", timeline.maxZoom.toDouble())
        root.put("maxX", timeline.maxX.toDouble())
        root.put("maxY", timeline.maxY.toDouble())
        root.put("maxRotation", timeline.maxRotation.toDouble())

        val samplesArr = org.json.JSONArray()
        for (s in timeline.samples) {
            val sObj = org.json.JSONObject()
            sObj.put("t", s.timestampMs)
            sObj.put("normT", s.normalizedTime.toDouble())
            sObj.put("scale", s.scale.toDouble())
            sObj.put("tx", s.translationX.toDouble())
            sObj.put("ty", s.translationY.toDouble())
            sObj.put("rot", s.rotationDegrees.toDouble())
            sObj.put("vel", s.velocity.toDouble())
            sObj.put("acc", s.acceleration.toDouble())
            sObj.put("conf", s.confidence.toDouble())
            samplesArr.put(sObj)
        }
        root.put("samples", samplesArr)
        return root.toString()
    }

    fun deserializeMotionTimeline(jsonStr: String?): MotionTimeline? {
        if (jsonStr.isNullOrBlank()) return null
        return try {
            val root = org.json.JSONObject(jsonStr)
            val durationMs = root.optLong("durationMs", 10000L)
            val sourceFps = root.optDouble("sourceFps", 30.0).toFloat()
            val analysisConfidence = root.optDouble("analysisConfidence", 0.95).toFloat()
            val avgMotion = root.optDouble("avgMotion", 0.0).toFloat()
            val maxZoom = root.optDouble("maxZoom", 1.0).toFloat()
            val maxX = root.optDouble("maxX", 0.0).toFloat()
            val maxY = root.optDouble("maxY", 0.0).toFloat()
            val maxRotation = root.optDouble("maxRotation", 0.0).toFloat()

            val samples = mutableListOf<com.example.data.model.MotionSample>()
            val samplesArr = root.optJSONArray("samples")
            if (samplesArr != null) {
                for (i in 0 until samplesArr.length()) {
                    val sObj = samplesArr.getJSONObject(i)
                    samples.add(
                        com.example.data.model.MotionSample(
                            timestampMs = sObj.optLong("t", 0L),
                            normalizedTime = sObj.optDouble("normT", 0.0).toFloat(),
                            scale = sObj.optDouble("scale", 1.0).toFloat(),
                            translationX = sObj.optDouble("tx", 0.0).toFloat(),
                            translationY = sObj.optDouble("ty", 0.0).toFloat(),
                            rotationDegrees = sObj.optDouble("rot", 0.0).toFloat(),
                            velocity = sObj.optDouble("vel", 0.0).toFloat(),
                            acceleration = sObj.optDouble("acc", 0.0).toFloat(),
                            confidence = sObj.optDouble("conf", 1.0).toFloat()
                        )
                    )
                }
            }
            MotionTimeline(
                durationMs = durationMs,
                sourceFps = sourceFps,
                samples = samples,
                analysisConfidence = analysisConfidence,
                avgMotion = avgMotion,
                maxZoom = maxZoom,
                maxX = maxX,
                maxY = maxY,
                maxRotation = maxRotation
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun KeyframeEntity.toKeyframe(): Keyframe {
        val easingType = try {
            EasingType.valueOf(easing)
        } catch (_: Exception) {
            EasingType.SMOOTH
        }
        val mType = try {
            MotionType.valueOf(motionType)
        } catch (_: Exception) {
            MotionType.NONE
        }
        return Keyframe(
            id = id,
            timestampMs = timestampMs,
            x = x,
            y = y,
            scale = scale,
            rotation = rotation,
            easing = easingType,
            motionType = mType,
            confidence = confidence
        )
    }
}

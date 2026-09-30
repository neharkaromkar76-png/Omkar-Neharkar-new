package com.example.data.local

import com.example.data.local.dao.KeyframeDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.entity.KeyframeEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.model.EasingType
import com.example.data.model.Keyframe
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

package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.KeyframeEntity
import com.example.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Dao
interface KeyframeDao {
    @Query("SELECT * FROM keyframes WHERE projectId = :projectId ORDER BY timestampMs ASC")
    fun getKeyframesForProject(projectId: Long): Flow<List<KeyframeEntity>>

    @Query("SELECT * FROM keyframes WHERE projectId = :projectId ORDER BY timestampMs ASC")
    suspend fun getKeyframesListForProject(projectId: Long): List<KeyframeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyframes(keyframes: List<KeyframeEntity>)

    @Query("DELETE FROM keyframes WHERE projectId = :projectId")
    suspend fun deleteKeyframesForProject(projectId: Long)
}

package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val referenceUri: String? = null,
    val targetUri: String? = null,
    val referenceDurationMs: Long = 0,
    val targetDurationMs: Long = 0,
    val referenceWidth: Int = 0,
    val referenceHeight: Int = 0,
    val targetWidth: Int = 0,
    val targetHeight: Int = 0,
    val status: String = "DRAFT", // DRAFT, ANALYZED, KEYFRAMED, RENDERED
    val renderedVideoPath: String? = null,
    val timingMode: String = "NORMALIZE",
    val motionIntensity: Float = 1.0f
)

@Entity(
    tableName = "keyframes",
    primaryKeys = ["id", "projectId"]
)
data class KeyframeEntity(
    val id: String,
    val projectId: Long,
    val timestampMs: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotation: Float,
    val easing: String,
    val motionType: String,
    val confidence: Float
)

package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {
    val allVideos: Flow<List<VideoEntity>> = db.videoDao().getAllVideos()

    suspend fun insertVideo(video: VideoEntity) = db.videoDao().insertVideo(video)
    suspend fun deleteVideo(video: VideoEntity) = db.videoDao().deleteVideo(video)

    suspend fun getConfig(key: String): String? = db.configDao().getConfig(key)?.value
    suspend fun setConfig(key: String, value: String) = db.configDao().setConfig(ConfigEntity(key, value))
    suspend fun deleteConfig(key: String) = db.configDao().deleteConfig(key)
}

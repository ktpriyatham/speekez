package com.speekez.data.dao

import androidx.room.*
import com.speekez.data.entity.Transcription
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY created_at DESC")
    fun getAllTranscriptions(): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE preset_id = :presetId ORDER BY created_at DESC")
    fun getTranscriptionsByPreset(presetId: Long): Flow<List<Transcription>>

    @Query("SELECT * FROM transcriptions WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteTranscriptions(): Flow<List<Transcription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(transcription: Transcription): Long

    @Delete
    suspend fun deleteTranscription(transcription: Transcription)

    @Query("UPDATE transcriptions SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
}

package com.guptrix.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllActiveNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getTrashNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE noteId = :noteId LIMIT 1")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE :query OR content LIKE :query) ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getFavoriteNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDraft = 1 AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getDraftNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE noteId = :noteId")
    suspend fun setPinned(noteId: String, isPinned: Boolean)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt WHERE noteId = :noteId")
    suspend fun moveToTrash(noteId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL WHERE noteId = :noteId")
    suspend fun restoreFromTrash(noteId: String)

    @Query("DELETE FROM notes WHERE noteId = :noteId")
    suspend fun permanentDelete(noteId: String)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :cutoffTimestamp")
    suspend fun purgeExpiredTrash(cutoffTimestamp: Long)

    @Query("DELETE FROM notes")
    suspend fun clearAll()

    @Query("DELETE FROM notes WHERE isDraft = 1")
    suspend fun clearDrafts()

    @Query("UPDATE notes SET isFavorite = :isFav WHERE noteId = :noteId")
    suspend fun toggleFavorite(noteId: String, isFav: Boolean)
}

package com.guptrix.app.repository

import com.guptrix.app.crypto.CryptoManager
import com.guptrix.app.database.NoteDao
import com.guptrix.app.database.NoteEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NoteRepository(
    private val noteDao: NoteDao,
    val cryptoManager: CryptoManager = CryptoManager()
) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()
    val trashNotes: Flow<List<NoteEntity>> = noteDao.getTrashNotes()
    val favorites: Flow<List<NoteEntity>> = noteDao.getFavoriteNotes()
    val drafts: Flow<List<NoteEntity>> = noteDao.getDraftNotes()

    suspend fun getNoteById(noteId: String): NoteEntity? = noteDao.getNoteById(noteId)

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        val formattedQuery = "%${query.trim()}%"
        return noteDao.searchNotes(formattedQuery)
    }

    suspend fun saveNote(
        title: String,
        content: String = "",
        canvasData: String? = null,
        color: String = "default",
        isPinned: Boolean = false,
        noteId: String = UUID.randomUUID().toString()
    ): NoteEntity {
        val now = System.currentTimeMillis()
        val existing = noteDao.getNoteById(noteId)
        val note = existing?.copy(
            title = title,
            content = content,
            canvasData = canvasData,
            color = color,
            isPinned = isPinned,
            updatedAt = now
        ) ?: NoteEntity(
            noteId = noteId,
            title = title,
            content = content,
            canvasData = canvasData,
            color = color,
            isPinned = isPinned,
            createdAt = now,
            updatedAt = now
        )
        noteDao.insertNote(note)
        return note
    }

    suspend fun insertRaw(note: NoteEntity) = noteDao.insertNote(note)
    suspend fun insert(note: NoteEntity) = noteDao.insertNote(note)
    suspend fun clearAll() = noteDao.clearAll()
    suspend fun clearDrafts() = noteDao.clearDrafts()
    suspend fun toggleFavorite(noteId: String, isFav: Boolean) = noteDao.toggleFavorite(noteId, isFav)

    suspend fun togglePin(noteId: String, isPinned: Boolean) {
        noteDao.setPinned(noteId, isPinned)
    }

    suspend fun moveToTrash(noteId: String) {
        noteDao.moveToTrash(noteId)
    }

    suspend fun restoreFromTrash(noteId: String) {
        noteDao.restoreFromTrash(noteId)
    }

    suspend fun permanentDelete(noteId: String) {
        noteDao.permanentDelete(noteId)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }
}

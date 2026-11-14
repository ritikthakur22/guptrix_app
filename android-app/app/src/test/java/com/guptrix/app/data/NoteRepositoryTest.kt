package com.guptrix.app.data

import com.guptrix.app.database.NoteDao
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteRepositoryTest {

    private lateinit var fakeNoteDao: FakeNoteDao
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        fakeNoteDao = FakeNoteDao()
        repository = NoteRepository(fakeNoteDao)
    }

    @Test
    fun testSaveAndRetrieveNote() = runBlocking {
        val saved = repository.saveNote(
            title = "Meeting Notes",
            content = "Discuss Q4 roadmap",
            color = "blue",
            isPinned = true
        )

        assertNotNull(saved.noteId)
        assertEquals("Meeting Notes", saved.title)
        assertEquals("Discuss Q4 roadmap", saved.content)
        assertTrue(saved.isPinned)

        val retrieved = repository.getNoteById(saved.noteId)
        assertNotNull(retrieved)
        assertEquals(saved.noteId, retrieved?.noteId)
    }

    @Test
    fun testMoveToTrashAndRestore() = runBlocking {
        val note = repository.saveNote("To be deleted", "temp content")
        repository.moveToTrash(note.noteId)

        val inTrash = repository.getNoteById(note.noteId)
        assertTrue(inTrash?.isDeleted == true)
        assertNotNull(inTrash?.deletedAt)

        repository.restoreFromTrash(note.noteId)
        val restored = repository.getNoteById(note.noteId)
        assertTrue(restored?.isDeleted == false)
        assertNull(restored?.deletedAt)
    }

    @Test
    fun testPermanentDelete() = runBlocking {
        val note = repository.saveNote("Permanent delete", "goodbye")
        repository.permanentDelete(note.noteId)

        val deleted = repository.getNoteById(note.noteId)
        assertNull(deleted)
    }

    @Test
    fun testSearchNotes() = runBlocking {
        repository.saveNote("Alpha Project", "Top secret")
        repository.saveNote("Beta Project", "Public discussion")

        val results = repository.searchNotes("Alpha").first()
        assertEquals(1, results.size)
        assertEquals("Alpha Project", results[0].title)
    }

    class FakeNoteDao : NoteDao {
        private val storage = mutableMapOf<String, NoteEntity>()

        override fun getAllActiveNotes(): Flow<List<NoteEntity>> =
            flowOf(storage.values.filter { !it.isDeleted }.sortedByDescending { it.updatedAt })

        override fun getTrashNotes(): Flow<List<NoteEntity>> =
            flowOf(storage.values.filter { it.isDeleted }.sortedByDescending { it.deletedAt ?: 0L })

        override suspend fun getNoteById(noteId: String): NoteEntity? = storage[noteId]

        override fun searchNotes(query: String): Flow<List<NoteEntity>> {
            val q = query.replace("%", "").lowercase()
            return flowOf(storage.values.filter {
                !it.isDeleted && (it.title.lowercase().contains(q) || it.content.lowercase().contains(q))
            })
        }

        override fun getFavoriteNotes(): Flow<List<NoteEntity>> =
            flowOf(storage.values.filter { !it.isDeleted && it.isFavorite })

        override fun getDraftNotes(): Flow<List<NoteEntity>> =
            flowOf(storage.values.filter { !it.isDeleted && it.isDraft })

        override suspend fun insertNote(note: NoteEntity) {
            storage[note.noteId] = note
        }

        override suspend fun insertAll(notes: List<NoteEntity>) {
            notes.forEach { storage[it.noteId] = it }
        }

        override suspend fun updateNote(note: NoteEntity) {
            storage[note.noteId] = note
        }

        override suspend fun setPinned(noteId: String, isPinned: Boolean) {
            storage[noteId]?.let { storage[noteId] = it.copy(isPinned = isPinned) }
        }

        override suspend fun moveToTrash(noteId: String, deletedAt: Long) {
            storage[noteId]?.let { storage[noteId] = it.copy(isDeleted = true, deletedAt = deletedAt) }
        }

        override suspend fun restoreFromTrash(noteId: String) {
            storage[noteId]?.let { storage[noteId] = it.copy(isDeleted = false, deletedAt = null) }
        }

        override suspend fun permanentDelete(noteId: String) {
            storage.remove(noteId)
        }

        override suspend fun emptyTrash() {
            storage.entries.removeIf { it.value.isDeleted }
        }

        override suspend fun purgeExpiredTrash(cutoffTimestamp: Long) {
            storage.entries.removeIf { it.value.isDeleted && (it.value.deletedAt ?: 0) < cutoffTimestamp }
        }

        override suspend fun clearAll() {
            storage.clear()
        }

        override suspend fun clearDrafts() {
            storage.entries.removeIf { it.value.isDraft }
        }

        override suspend fun toggleFavorite(noteId: String, isFav: Boolean) {
            storage[noteId]?.let { storage[noteId] = it.copy(isFavorite = isFav) }
        }
    }
}

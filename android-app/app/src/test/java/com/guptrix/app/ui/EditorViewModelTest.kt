package com.guptrix.app.ui

import com.guptrix.app.database.NoteDao
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import com.guptrix.app.ui.editor.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDao
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeDao()
        repository = NoteRepository(fakeDao)
        viewModel = EditorViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadExistingNote() = runTest(testDispatcher) {
        val note = NoteEntity(
            noteId = "test-123",
            title = "Existing Note",
            content = "Secret Body",
            color = "blue",
            isPinned = true
        )
        fakeDao.insertNote(note)

        viewModel.loadNote("test-123")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("test-123", state.noteId)
        assertEquals("Existing Note", state.title)
        assertEquals("Secret Body", state.content)
        assertEquals("blue", state.color)
        assertTrue(state.isPinned)
    }

    @Test
    fun testUpdateTitleAndContentAutoSaves() = runTest(testDispatcher) {
        viewModel.loadNote(null) // New note
        advanceUntilIdle()

        val newId = viewModel.uiState.value.noteId
        assertNotNull(newId)

        viewModel.updateTitle("Draft Title")
        viewModel.updateContent("Draft Content")
        viewModel.saveNoteNow()
        advanceUntilIdle()

        val saved = repository.getNoteById(newId)
        assertNotNull(saved)
        assertEquals("Draft Title", saved?.title)
        assertEquals("Draft Content", saved?.content)
    }

    @Test
    fun testTogglePinAndColor() = runTest(testDispatcher) {
        viewModel.loadNote(null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isPinned)
        viewModel.togglePin()
        assertTrue(viewModel.uiState.value.isPinned)

        viewModel.updateColor("purple")
        assertEquals("purple", viewModel.uiState.value.color)
    }

    @Test
    fun testCanvasToggle() = runTest(testDispatcher) {
        viewModel.loadNote(null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isCanvasVisible)
        viewModel.toggleCanvas()
        assertTrue(viewModel.uiState.value.isCanvasVisible)
    }

    class FakeDao : NoteDao {
        private val list = mutableListOf<NoteEntity>()
        private val flow = MutableStateFlow<List<NoteEntity>>(emptyList())

        override fun getAllActiveNotes(): Flow<List<NoteEntity>> = flow
        override fun getTrashNotes(): Flow<List<NoteEntity>> = flow
        override suspend fun getNoteById(noteId: String): NoteEntity? = list.find { it.noteId == noteId }
        override fun searchNotes(query: String): Flow<List<NoteEntity>> = flow
        override fun getFavoriteNotes(): Flow<List<NoteEntity>> = flow
        override fun getDraftNotes(): Flow<List<NoteEntity>> = flow
        override suspend fun insertNote(note: NoteEntity) {
            list.removeAll { it.noteId == note.noteId }
            list.add(note)
            flow.value = list.toList()
        }
        override suspend fun insertAll(notes: List<NoteEntity>) {
            list.addAll(notes)
            flow.value = list.toList()
        }
        override suspend fun updateNote(note: NoteEntity) {
            list.removeAll { it.noteId == note.noteId }
            list.add(note)
            flow.value = list.toList()
        }
        override suspend fun setPinned(noteId: String, isPinned: Boolean) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isPinned = isPinned)
                flow.value = list.toList()
            }
        }
        override suspend fun moveToTrash(noteId: String, deletedAt: Long) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isDeleted = true, deletedAt = deletedAt)
                flow.value = list.toList()
            }
        }
        override suspend fun restoreFromTrash(noteId: String) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isDeleted = false, deletedAt = null)
                flow.value = list.toList()
            }
        }
        override suspend fun permanentDelete(noteId: String) {
            list.removeAll { it.noteId == noteId }
            flow.value = list.toList()
        }
        override suspend fun emptyTrash() {
            list.removeAll { it.isDeleted }
            flow.value = list.toList()
        }
        override suspend fun purgeExpiredTrash(cutoffTimestamp: Long) {
            list.removeAll { it.isDeleted && (it.deletedAt ?: 0L) < cutoffTimestamp }
            flow.value = list.toList()
        }
        override suspend fun clearAll() {
            list.clear()
            flow.value = emptyList()
        }
        override suspend fun clearDrafts() {
            list.removeAll { it.isDraft }
            flow.value = list.toList()
        }
        override suspend fun toggleFavorite(noteId: String, isFav: Boolean) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isFavorite = isFav)
                flow.value = list.toList()
            }
        }
    }
}

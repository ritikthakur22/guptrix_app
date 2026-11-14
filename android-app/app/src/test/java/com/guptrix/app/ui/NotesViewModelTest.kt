package com.guptrix.app.ui

import com.guptrix.app.database.NoteDao
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import com.guptrix.app.ui.components.NoteFilter
import com.guptrix.app.ui.dashboard.NotesViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeDao
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeDao()
        repository = NoteRepository(fakeDao)
        viewModel = NotesViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateLoadsNotes() = runTest(testDispatcher) {
        val note1 = NoteEntity(noteId = "1", title = "Alpha", content = "Content 1", isPinned = false)
        val note2 = NoteEntity(noteId = "2", title = "Beta", content = "Content 2", isPinned = true)
        fakeDao.emitNotes(listOf(note1, note2))

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.notes.size)
        // Pinned notes should come first
        assertEquals("2", state.notes[0].noteId)
        assertEquals("1", state.notes[1].noteId)
    }

    @Test
    fun testFilterByPinnedNotes() = runTest(testDispatcher) {
        val note1 = NoteEntity(noteId = "1", title = "Alpha", content = "Content 1", isPinned = false)
        val note2 = NoteEntity(noteId = "2", title = "Beta", content = "Content 2", isPinned = true)
        fakeDao.emitNotes(listOf(note1, note2))

        advanceUntilIdle()

        viewModel.onFilterChanged(NoteFilter.PINNED)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.notes.size)
        assertEquals("2", state.notes[0].noteId)
    }

    @Test
    fun testSearchQueryFiltersNotes() = runTest(testDispatcher) {
        val note1 = NoteEntity(noteId = "1", title = "Secret Code", content = "1234")
        val note2 = NoteEntity(noteId = "2", title = "Grocery List", content = "Apples")
        fakeDao.emitNotes(listOf(note1, note2))

        advanceUntilIdle()

        viewModel.onSearchQueryChanged("secret")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.notes.size)
        assertEquals("Secret Code", state.notes[0].title)
    }

    @Test
    fun testDismissVaultBanner() = runTest(testDispatcher) {
        assertTrue(viewModel.uiState.value.showVaultBanner)
        viewModel.dismissVaultBanner()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showVaultBanner)
    }

    class FakeDao : NoteDao {
        private val notesFlow = MutableStateFlow<List<NoteEntity>>(emptyList())
        private val list = mutableListOf<NoteEntity>()

        fun emitNotes(notes: List<NoteEntity>) {
            list.clear()
            list.addAll(notes)
            notesFlow.value = list.toList()
        }

        override fun getAllActiveNotes(): Flow<List<NoteEntity>> = notesFlow
        override fun getTrashNotes(): Flow<List<NoteEntity>> = MutableStateFlow(emptyList())
        override suspend fun getNoteById(noteId: String): NoteEntity? = list.find { it.noteId == noteId }
        override fun searchNotes(query: String): Flow<List<NoteEntity>> = notesFlow
        override fun getFavoriteNotes(): Flow<List<NoteEntity>> = notesFlow
        override fun getDraftNotes(): Flow<List<NoteEntity>> = notesFlow
        override suspend fun insertNote(note: NoteEntity) {
            list.removeAll { it.noteId == note.noteId }
            list.add(note)
            notesFlow.value = list.toList()
        }
        override suspend fun insertAll(notes: List<NoteEntity>) {
            list.addAll(notes)
            notesFlow.value = list.toList()
        }
        override suspend fun updateNote(note: NoteEntity) {
            list.removeAll { it.noteId == note.noteId }
            list.add(note)
            notesFlow.value = list.toList()
        }
        override suspend fun setPinned(noteId: String, isPinned: Boolean) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isPinned = isPinned)
                notesFlow.value = list.toList()
            }
        }
        override suspend fun moveToTrash(noteId: String, deletedAt: Long) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isDeleted = true, deletedAt = deletedAt)
                notesFlow.value = list.toList()
            }
        }
        override suspend fun restoreFromTrash(noteId: String) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isDeleted = false, deletedAt = null)
                notesFlow.value = list.toList()
            }
        }
        override suspend fun permanentDelete(noteId: String) {
            list.removeAll { it.noteId == noteId }
            notesFlow.value = list.toList()
        }
        override suspend fun emptyTrash() {
            list.removeAll { it.isDeleted }
            notesFlow.value = list.toList()
        }
        override suspend fun purgeExpiredTrash(cutoffTimestamp: Long) {
            list.removeAll { it.isDeleted && (it.deletedAt ?: 0L) < cutoffTimestamp }
            notesFlow.value = list.toList()
        }
        override suspend fun clearAll() {
            list.clear()
            notesFlow.value = emptyList()
        }
        override suspend fun clearDrafts() {
            list.removeAll { it.isDraft }
            notesFlow.value = list.toList()
        }
        override suspend fun toggleFavorite(noteId: String, isFav: Boolean) {
            val idx = list.indexOfFirst { it.noteId == noteId }
            if (idx != -1) {
                list[idx] = list[idx].copy(isFavorite = isFav)
                notesFlow.value = list.toList()
            }
        }
    }
}

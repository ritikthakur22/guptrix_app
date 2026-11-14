package com.guptrix.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import com.guptrix.app.ui.components.NoteFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: NoteFilter = NoteFilter.ALL,
    val isLoading: Boolean = false,
    val showVaultBanner: Boolean = true
)

class NotesViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(NoteFilter.ALL)
    private val _showVaultBanner = MutableStateFlow(true)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<NotesUiState> = combine(
        repository.activeNotes,
        _searchQuery,
        _selectedFilter,
        _showVaultBanner,
        _isLoading
    ) { notes, query, filter, showBanner, loading ->
        val filteredNotes = notes.filter { note ->
            val matchesFilter = when (filter) {
                NoteFilter.ALL -> true
                NoteFilter.PINNED -> note.isPinned
                NoteFilter.FAVORITES -> note.isFavorite
                NoteFilter.DRAFTS -> note.isDraft
            }

            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }.sortedWith(
            compareByDescending<NoteEntity> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )

        NotesUiState(
            notes = filteredNotes,
            searchQuery = query,
            selectedFilter = filter,
            isLoading = loading,
            showVaultBanner = showBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = NotesUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: NoteFilter) {
        _selectedFilter.value = filter
    }

    fun dismissVaultBanner() {
        _showVaultBanner.value = false
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note.noteId, !note.isPinned)
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(note.noteId, !note.isFavorite)
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            repository.moveToTrash(noteId)
        }
    }
}

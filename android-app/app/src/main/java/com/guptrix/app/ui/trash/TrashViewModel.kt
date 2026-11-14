package com.guptrix.app.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    val trashNotes: StateFlow<List<NoteEntity>> = repository.trashNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreNote(noteId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(noteId)
        }
    }

    fun permanentDelete(noteId: String) {
        viewModelScope.launch {
            repository.permanentDelete(noteId)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }
}

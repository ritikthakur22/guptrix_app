package com.guptrix.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditorUiState(
    val noteId: String = "",
    val title: String = "",
    val content: String = "",
    val canvasData: String? = null,
    val color: String = "default",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isCanvasVisible: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleted: Boolean = false
)

class EditorViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    fun loadNote(noteId: String?) {
        viewModelScope.launch {
            if (noteId.isNullOrBlank() || noteId == "new") {
                val newId = UUID.randomUUID().toString()
                _uiState.value = EditorUiState(noteId = newId)
            } else {
                val existing = repository.getNoteById(noteId)
                if (existing != null) {
                    _uiState.value = EditorUiState(
                        noteId = existing.noteId,
                        title = existing.title,
                        content = existing.content,
                        canvasData = existing.canvasData,
                        color = existing.color,
                        isPinned = existing.isPinned,
                        isFavorite = existing.isFavorite,
                        isCanvasVisible = !existing.canvasData.isNullOrBlank()
                    )
                } else {
                    _uiState.value = EditorUiState(noteId = noteId)
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateContent(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun updateCanvasData(json: String?) {
        _uiState.update { it.copy(canvasData = json) }
    }

    fun updateColor(newColor: String) {
        _uiState.update { it.copy(color = newColor) }
    }

    fun togglePin() {
        _uiState.update { it.copy(isPinned = !it.isPinned) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun toggleCanvas() {
        _uiState.update { it.copy(isCanvasVisible = !it.isCanvasVisible) }
    }

    fun saveNoteNow() {
        val current = _uiState.value
        if (current.noteId.isBlank()) return

        // Skip saving empty notes (no title, content, or canvas)
        if (current.title.isBlank() && current.content.isBlank() && current.canvasData.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.saveNote(
                noteId = current.noteId,
                title = current.title,
                content = current.content,
                canvasData = current.canvasData,
                color = current.color,
                isPinned = current.isPinned
            )
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun deleteNote() {
        val current = _uiState.value
        if (current.noteId.isNotBlank()) {
            viewModelScope.launch {
                repository.moveToTrash(current.noteId)
                _uiState.update { it.copy(isDeleted = true) }
            }
        }
    }
}

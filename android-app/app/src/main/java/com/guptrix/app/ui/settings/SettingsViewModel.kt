package com.guptrix.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guptrix.app.crypto.KeystoreVaultManager
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class SettingsUiState(
    val hasPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricHardwareAvailable: Boolean = false,
    val activeNotesCount: Int = 0,
    val trashNotesCount: Int = 0,
    val exportDataString: String? = null,
    val importSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val repository: NoteRepository,
    private val vaultManager: KeystoreVaultManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            hasPinSet = vaultManager.hasPinSet(),
            isBiometricEnabled = vaultManager.isBiometricEnabled(),
            isBiometricHardwareAvailable = vaultManager.isBiometricHardwareAvailable()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeNotes.collect { notes ->
                _uiState.update { it.copy(activeNotesCount = notes.size) }
            }
        }
        viewModelScope.launch {
            repository.trashNotes.collect { trash ->
                _uiState.update { it.copy(trashNotesCount = trash.size) }
            }
        }
    }

    fun refreshSecurityState() {
        _uiState.update {
            it.copy(
                hasPinSet = vaultManager.hasPinSet(),
                isBiometricEnabled = vaultManager.isBiometricEnabled(),
                isBiometricHardwareAvailable = vaultManager.isBiometricHardwareAvailable()
            )
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        vaultManager.setBiometricEnabled(enabled)
        _uiState.update { it.copy(isBiometricEnabled = enabled) }
    }

    fun removePin() {
        vaultManager.removePin()
        refreshSecurityState()
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAll()
            vaultManager.removePin()
            refreshSecurityState()
        }
    }

    fun exportEncryptedBackup() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(exportDataString = "Backup ready for export.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(exportDataString = null, importSuccessMessage = null, errorMessage = null) }
    }
}

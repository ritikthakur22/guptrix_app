package com.guptrix.app.ui.vault

import androidx.lifecycle.ViewModel
import com.guptrix.app.crypto.KeystoreVaultManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VaultUiState(
    val enteredPin: String = "",
    val firstEnteredPin: String = "",
    val isSettingUp: Boolean = false,
    val isConfirming: Boolean = false,
    val hasPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val errorMessage: String? = null,
    val isUnlocked: Boolean = false
)

class VaultViewModel(
    private val vaultManager: KeystoreVaultManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        VaultUiState(
            hasPinSet = vaultManager.hasPinSet(),
            isSettingUp = !vaultManager.hasPinSet(),
            isBiometricEnabled = vaultManager.isBiometricEnabled(),
            isBiometricAvailable = vaultManager.isBiometricHardwareAvailable()
        )
    )
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun onDigitClick(digit: String) {
        val current = _uiState.value
        if (current.enteredPin.length >= 4) return

        val newPin = current.enteredPin + digit
        _uiState.update { it.copy(enteredPin = newPin, errorMessage = null) }

        if (newPin.length == 4) {
            handleFourDigitsEntered(newPin)
        }
    }

    private fun handleFourDigitsEntered(pin: String) {
        val current = _uiState.value
        if (current.isSettingUp) {
            if (!current.isConfirming) {
                // First entry in setup, now ask for confirmation
                _uiState.update {
                    it.copy(
                        firstEnteredPin = pin,
                        enteredPin = "",
                        isConfirming = true,
                        errorMessage = null
                    )
                }
            } else {
                // Confirming step
                if (pin == current.firstEnteredPin) {
                    vaultManager.setupPin(pin)
                    _uiState.update {
                        it.copy(
                            isUnlocked = true,
                            hasPinSet = true,
                            isSettingUp = false,
                            isConfirming = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            enteredPin = "",
                            firstEnteredPin = "",
                            isConfirming = false,
                            errorMessage = "PINs do not match. Try again."
                        )
                    }
                }
            }
        } else {
            // Unlocking with existing PIN
            val success = vaultManager.verifyPin(pin)
            if (success) {
                _uiState.update { it.copy(isUnlocked = true, errorMessage = null) }
            } else {
                _uiState.update {
                    it.copy(
                        enteredPin = "",
                        errorMessage = "Incorrect PIN. Try again."
                    )
                }
            }
        }
    }

    fun onDeleteClick() {
        _uiState.update {
            if (it.enteredPin.isNotEmpty()) {
                it.copy(enteredPin = it.enteredPin.dropLast(1), errorMessage = null)
            } else {
                it
            }
        }
    }

    fun onBiometricSuccess() {
        vaultManager.unlockByBiometrics()
        _uiState.update { it.copy(isUnlocked = true, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

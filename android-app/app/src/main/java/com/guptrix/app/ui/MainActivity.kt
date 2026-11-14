package com.guptrix.app.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.guptrix.app.crypto.KeystoreVaultManager
import com.guptrix.app.database.AppDatabase
import com.guptrix.app.repository.NoteRepository
import com.guptrix.app.ui.navigation.GuptrixNavHost
import com.guptrix.app.ui.navigation.Screen
import com.guptrix.app.ui.theme.GuptrixTheme

class MainActivity : AppCompatActivity() {

    private lateinit var repository: NoteRepository
    private lateinit var vaultManager: KeystoreVaultManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLCipher Room database and repository
        val database = AppDatabase.getDatabase(this)
        repository = NoteRepository(database.noteDao())
        vaultManager = KeystoreVaultManager(this)

        // Cold-launch destination: If user has configured a PIN, lock vault on start
        val startDestination = if (vaultManager.hasPinSet() && !vaultManager.isUnlocked) {
            Screen.Vault.route
        } else {
            Screen.Dashboard.route
        }

        setContent {
            GuptrixTheme {
                GuptrixNavHost(
                    repository = repository,
                    vaultManager = vaultManager,
                    startDestination = startDestination,
                    onTriggerBiometrics = { triggerBiometricPrompt() }
                )
            }
        }
    }

    private fun triggerBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    vaultManager.unlockByBiometrics()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Guptrix Vault")
            .setSubtitle("Confirm your identity to decrypt your notes")
            .setNegativeButtonText("Use PIN")
            .build()

        prompt.authenticate(promptInfo)
    }
}

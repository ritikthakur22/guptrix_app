package com.guptrix.app.ui

import android.content.SharedPreferences
import com.guptrix.app.crypto.CryptoManager
import com.guptrix.app.crypto.KeystoreVaultManager
import com.guptrix.app.ui.vault.VaultViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VaultViewModelTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var vaultManager: KeystoreVaultManager
    private lateinit var viewModel: VaultViewModel

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        vaultManager = KeystoreVaultManager(prefs = fakePrefs, cryptoManager = CryptoManager())
        viewModel = VaultViewModel(vaultManager)
    }

    @Test
    fun testInitialSetupFlow() {
        val state = viewModel.uiState.value
        assertTrue(state.isSettingUp)
        assertFalse(state.hasPinSet)
        assertFalse(state.isConfirming)

        // Enter 1234
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("2")
        viewModel.onDigitClick("3")
        viewModel.onDigitClick("4")

        // Should now be confirming
        val confirmingState = viewModel.uiState.value
        assertTrue(confirmingState.isConfirming)
        assertEquals("", confirmingState.enteredPin)
        assertEquals("1234", confirmingState.firstEnteredPin)

        // Enter matching 1234
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("2")
        viewModel.onDigitClick("3")
        viewModel.onDigitClick("4")

        // Should now be unlocked
        val unlockedState = viewModel.uiState.value
        assertTrue(unlockedState.isUnlocked)
        assertTrue(unlockedState.hasPinSet)
        assertNull(unlockedState.errorMessage)
    }

    @Test
    fun testSetupMismatchShowsError() {
        // Enter 1234
        viewModel.onDigitClick("1")
        viewModel.onDigitClick("2")
        viewModel.onDigitClick("3")
        viewModel.onDigitClick("4")

        // Enter mismatch 9999
        viewModel.onDigitClick("9")
        viewModel.onDigitClick("9")
        viewModel.onDigitClick("9")
        viewModel.onDigitClick("9")

        val state = viewModel.uiState.value
        assertFalse(state.isUnlocked)
        assertFalse(state.isConfirming)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun testDeleteDigit() {
        viewModel.onDigitClick("5")
        viewModel.onDigitClick("6")
        assertEquals("56", viewModel.uiState.value.enteredPin)

        viewModel.onDeleteClick()
        assertEquals("5", viewModel.uiState.value.enteredPin)
    }

    class FakeSharedPreferences : SharedPreferences {
        private val data = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            data[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(data)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
            private val temp = mutableMapOf<String, Any?>()
            private val removed = mutableSetOf<String>()

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null) temp[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) temp[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) removed.add(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                storage.clear()
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                removed.forEach { storage.remove(it) }
                storage.putAll(temp)
            }
        }
    }
}

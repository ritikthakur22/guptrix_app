# Guptrix Native Jetpack Compose Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild Guptrix from a hybrid WebView wrapper into a 100% native Android note-taking application using Jetpack Compose, SQLCipher Room storage, and hardware-backed biometric security.

**Architecture:** MVVM architecture with Jetpack Compose for declarative UI, Room SQLite encrypted via SQLCipher for local-first storage, AES-256-GCM envelope encryption for sensitive payload data, and Android BiometricPrompt with Keystore for instant vault unlocking.

**Tech Stack:** Kotlin 2.0, Jetpack Compose, Material 3, AndroidX Room 2.6, SQLCipher 4.5.4, AndroidX Biometric 1.2, AndroidX Security Crypto 1.1, Coroutines & StateFlow.

**Spec:** [docs/superpowers/specs/2026-11-14-guptrix-native-compose-design.md](file:///home/crdy/testing/test/guptrix_app/docs/superpowers/specs/2026-11-14-guptrix-native-compose-design.md)

---

## Global Constraints

- Android API 26 minimum, API 35/36 compile/target.
- No WebViews or hybrid bridges remaining in production code.
- Zero plaintext persistence of unencrypted note payloads or master credentials on disk.
- Pure Kotlin Compose UI with zero legacy XML fragment layouts for new screens.
- Hallmark compliance: Obsidian dark theme (`#101216`), tokenized colors, no purple-blue gradients, 1dp subtle borders, no card-in-card nesting.
- All commits authored as `Ritik Thakur <188522288+ritikthakur22@users.noreply.github.com>`.

## Review Focus

- Corrupted or tampered ciphertext: `CryptoManager` throws `AEADBadTagException` and surfaces a safe decryption error instead of crashing the app.
- Rapid search input while notes are updating: Flow debounce prevents race conditions in search queries.
- Biometric hardware unavailable or not enrolled: Graceful fallback to numeric master PIN without crash.
- Canvas drawing with empty or zero strokes: Serializer cleanly handles null/empty stroke lists without throwing JSON exceptions.
- Database migration on legacy device: `fallbackToDestructiveMigration()` triggers cleanly without Room SQLite schema mismatch exceptions.

---

### Task 1: Build System & Dependencies Configuration

**Files:**
- Modify: `android-app/app/build.gradle.kts`
- Modify: `android-app/build.gradle.kts`
- Modify: `android-app/gradle/libs.versions.toml` (if present)

**Interfaces:**
- Consumes: None
- Produces: Compose compiler plugins, Material 3 BOM, SQLCipher, Biometric, and Security dependencies accessible across the app module.

- [ ] **Step 1: Update `android-app/app/build.gradle.kts` to enable Compose and add dependencies**

Add `buildFeatures { compose = true }`, `composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }`, and implementation dependencies for Compose BOM (`2024.09.00`), Material 3, SQLCipher (`net.zetetic:android-database-sqlcipher:4.5.4`), Biometric KTX (`androidx.biometric:biometric-ktx:1.2.0-alpha05`), and Security Crypto (`androidx.security:security-crypto:1.1.0-alpha06`).

- [ ] **Step 2: Run Gradle sync / compile check**

Run: `cd android-app && ./gradlew tasks --dry-run`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit build configuration changes**

```bash
git add android-app/app/build.gradle.kts
git commit -m "build: configure Jetpack Compose, SQLCipher and Biometric dependencies"
```

---

### Task 2: Cryptographic Core & Payload Encryption (TDD)

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/crypto/CryptoManager.kt`
- Create: `android-app/app/src/test/java/com/guptrix/app/crypto/CryptoManagerTest.kt`

**Interfaces:**
- Consumes: Standard `javax.crypto.Cipher`, `java.security.SecureRandom`
- Produces:
  ```kotlin
  data class EncryptedPayload(val ciphertextBase64: String, val ivBase64: String)
  class CryptoManager {
      fun encrypt(plaintext: String, key: SecretKey): EncryptedPayload
      fun decrypt(payload: EncryptedPayload, key: SecretKey): String
      fun generateKey(): SecretKey
      fun deriveKeyFromPassphrase(passphrase: String, salt: ByteArray): SecretKey
  }
  ```

- [ ] **Step 1: Write failing tests in `CryptoManagerTest.kt`**

Write tests verifying:
- Encryption and decryption round-trip returns exact original string.
- Decrypting with wrong key throws exception.
- Decrypting tampered ciphertext throws exception.
- PBKDF2 key derivation produces 256-bit AES key.

- [ ] **Step 2: Run tests to verify failure**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.crypto.CryptoManagerTest`
Expected: FAIL (unresolved reference CryptoManager)

- [ ] **Step 3: Implement `CryptoManager.kt`**

Implement AES/GCM/NoPadding (12-byte IV, 128-bit tag) and PBKDF2WithHmacSHA256 (100,000 rounds, 256-bit output) in `com.guptrix.app.crypto.CryptoManager`.

- [ ] **Step 4: Run tests to verify pass**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.crypto.CryptoManagerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/crypto/CryptoManager.kt android-app/app/src/test/java/com/guptrix/app/crypto/CryptoManagerTest.kt
git commit -m "feat(crypto): implement AES-256-GCM and PBKDF2 encryption engine"
```

---

### Task 3: Room Database with SQLCipher & Note Repository (TDD)

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/data/local/NoteEntity.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/data/local/NoteDao.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/data/local/GuptrixDatabase.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/data/repository/NoteRepository.kt`
- Create: `android-app/app/src/test/java/com/guptrix/app/data/NoteRepositoryTest.kt`

**Interfaces:**
- Consumes: `CryptoManager`
- Produces:
  ```kotlin
  class NoteRepository(val noteDao: NoteDao, val cryptoManager: CryptoManager) {
      fun getActiveNotes(): Flow<List<NoteEntity>>
      fun getTrashNotes(): Flow<List<NoteEntity>>
      fun searchNotes(query: String): Flow<List<NoteEntity>>
      suspend fun saveNote(note: NoteEntity)
      suspend fun moveToTrash(noteId: String)
      suspend fun restoreFromTrash(noteId: String)
      suspend fun permanentDelete(noteId: String)
  }
  ```

- [ ] **Step 1: Write failing test in `NoteRepositoryTest.kt`**

Write tests for inserting notes, filtering active vs trash notes, updating note content, and soft-delete/restore operations.

- [ ] **Step 2: Run test to verify failure**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.data.NoteRepositoryTest`
Expected: FAIL

- [ ] **Step 3: Implement Entity, DAO, Database and Repository**

Implement `NoteEntity`, `NoteDao` with reactive SQLite queries, `GuptrixDatabase` with SQLCipher OpenHelperFactory, and `NoteRepository`.

- [ ] **Step 4: Run test to verify pass**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.data.NoteRepositoryTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/data/
git commit -m "feat(data): implement SQLCipher-backed Room database and NoteRepository"
```

---

### Task 4: Material 3 Design System & Shared Components

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/theme/Color.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/theme/Type.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/theme/Theme.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/components/NoteCard.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/components/TopSearchBar.kt`

**Interfaces:**
- Consumes: Compose Material 3
- Produces: `GuptrixTheme`, `NoteCard(note, onClick, onPinClick)`, `TopSearchBar(query, onQueryChange, selectedFilter, onFilterChange)`.

- [ ] **Step 1: Implement Theme tokens in `Color.kt`, `Type.kt`, and `Theme.kt`**

Define Obsidian background (`#101216`), Surface (`#181B20`), Border (`#282D37`), Accent (`#3B82F6`), pastel color chips, and crisp typography scale.

- [ ] **Step 2: Implement `NoteCard.kt` and `TopSearchBar.kt`**

Create clean note cards with 1dp border, color accent chip, pin status badge, and title/preview text.

- [ ] **Step 3: Verify compilation**

Run: `cd android-app && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/ui/theme/ android-app/app/src/main/java/com/guptrix/app/ui/components/
git commit -m "feat(ui): implement Obsidian Material 3 theme tokens and shared components"
```

---

### Task 5: Notes Dashboard Screen

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/dashboard/NotesDashboardScreen.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/dashboard/NotesViewModel.kt`
- Create: `android-app/app/src/test/java/com/guptrix/app/ui/NotesViewModelTest.kt`

**Interfaces:**
- Consumes: `NoteRepository`, `GuptrixTheme`, `NoteCard`, `TopSearchBar`
- Produces: `NotesDashboardScreen(onNoteClick, onNewNoteClick, onSettingsClick, onTrashClick)`

- [ ] **Step 1: Write ViewModel unit test in `NotesViewModelTest.kt`**

Verify active notes emission, search filtering, and pin toggling updates StateFlow.

- [ ] **Step 2: Run test to verify failure**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.ui.NotesViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement `NotesViewModel.kt` and `NotesDashboardScreen.kt`**

Implement staggered vertical grid, floating action button for new note, empty state illustration, and dismissible onboarding vault banner.

- [ ] **Step 4: Run test to verify pass**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.ui.NotesViewModelTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/ui/dashboard/ android-app/app/src/test/java/com/guptrix/app/ui/NotesViewModelTest.kt
git commit -m "feat(ui): implement NotesDashboardScreen with staggered grid and search filtering"
```

---

### Task 6: Note Editor Screen & Native Drawing Canvas

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/editor/NoteEditorScreen.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/editor/CanvasDrawingView.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/editor/EditorViewModel.kt`
- Create: `android-app/app/src/test/java/com/guptrix/app/ui/EditorViewModelTest.kt`

**Interfaces:**
- Consumes: `NoteRepository`
- Produces: `NoteEditorScreen(noteId, onBackClick)`

- [ ] **Step 1: Write ViewModel unit test in `EditorViewModelTest.kt`**

Test note loading, title change, content change, stroke serialization to JSON, and auto-save on debounce.

- [ ] **Step 2: Run test to verify failure**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.ui.EditorViewModelTest`
Expected: FAIL

- [ ] **Step 3: Implement `CanvasDrawingView.kt`, `EditorViewModel.kt`, and `NoteEditorScreen.kt`**

Implement smooth touch stroke capture on Compose `Canvas`, color pallet switcher, title and body text fields with auto-save.

- [ ] **Step 4: Run test to verify pass**

Run: `cd android-app && ./gradlew testDebugUnitTest --tests com.guptrix.app.ui.EditorViewModelTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/ui/editor/ android-app/app/src/test/java/com/guptrix/app/ui/EditorViewModelTest.kt
git commit -m "feat(editor): implement NoteEditorScreen and native Compose drawing canvas"
```

---

### Task 7: Vault Lock & Biometric Keystore Integration

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/crypto/KeystoreVaultManager.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/vault/VaultLockScreen.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/vault/VaultViewModel.kt`

**Interfaces:**
- Consumes: Android Keystore, `BiometricPrompt`
- Produces: `KeystoreVaultManager.unlockWithBiometrics()`, `VaultLockScreen(onUnlocked)`

- [ ] **Step 1: Implement `KeystoreVaultManager.kt`**

Manage Android Keystore master key wrapping and biometric prompt cipher initialization.

- [ ] **Step 2: Implement `VaultLockScreen.kt` and `VaultViewModel.kt`**

Create clean 4-6 digit numeric keypad entry with biometric shortcut trigger button.

- [ ] **Step 3: Verify compilation**

Run: `cd android-app && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/crypto/KeystoreVaultManager.kt android-app/app/src/main/java/com/guptrix/app/ui/vault/
git commit -m "feat(vault): implement biometric unlock and master PIN lock screen"
```

---

### Task 8: Trash & Settings Screens

**Files:**
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/trash/TrashScreen.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/settings/SettingsScreen.kt`

**Interfaces:**
- Consumes: `NoteRepository`, `KeystoreVaultManager`
- Produces: `TrashScreen(onRestore, onPermanentDelete, onBack)`, `SettingsScreen(onToggleBiometrics, onExportBackup, onBack)`

- [ ] **Step 1: Implement `TrashScreen.kt`**

Display deleted notes with recovery actions and empty-trash confirmation dialog.

- [ ] **Step 2: Implement `SettingsScreen.kt`**

Display security settings (PIN change, Biometrics toggle), database export/import actions, and app metadata.

- [ ] **Step 3: Verify compilation**

Run: `cd android-app && ./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add android-app/app/src/main/java/com/guptrix/app/ui/trash/ android-app/app/src/main/java/com/guptrix/app/ui/settings/
git commit -m "feat(ui): implement Trash recovery screen and Vault Settings screen"
```

---

### Task 9: Legacy Purge, Navigation Wiring & End-to-End Build Verification

**Files:**
- Delete: `android-app/app/src/main/java/com/guptrix/app/WebViewManager.kt`
- Delete: `android-app/app/src/main/res/layout/*`
- Modify: `android-app/app/src/main/java/com/guptrix/app/ui/MainActivity.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/navigation/GuptrixNavHost.kt`
- Create: `android-app/app/src/main/java/com/guptrix/app/ui/navigation/Screen.kt`

**Interfaces:**
- Consumes: All UI screens
- Produces: Single Activity hosting full Compose navigation graph.

- [ ] **Step 1: Delete obsolete WebView, legacy XML layouts, and stub fragments**

Remove `WebViewManager.kt`, `activity_main.xml`, `fragment_*.xml`, and unused legacy fragments.

- [ ] **Step 2: Implement Navigation Graph in `GuptrixNavHost.kt` and update `MainActivity.kt`**

Set `MainActivity` content to `GuptrixApp()` with routes: `Dashboard`, `Editor/{noteId}`, `VaultLock`, `Trash`, `Settings`.

- [ ] **Step 3: Run full verification suite**

Run: `cd android-app && ./gradlew testDebugUnitTest && ./gradlew assembleDebug`
Expected: All unit tests pass and debug APK compiles cleanly (`assembleDebug` SUCCESSFUL).

- [ ] **Step 4: Commit and push feature branch**

```bash
git add -A
git commit -m "refactor: replace legacy WebView and XML layout system with native Compose architecture"
git push origin feature/native-compose-app
```

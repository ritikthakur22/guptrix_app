# Guptrix Native Android Redesign Document

**Date:** 2026-11-14  
**Status:** Draft for Review  
**Target Platform:** Android 8.0+ (API 26 to 35)  
**Architecture:** Jetpack Compose + MVVM + Room SQLite + Android Keystore Biometrics  

---

## 1. Intent and Context

Guptrix is an encrypted, privacy-focused note-taking application. Previously, the Android application wrapped a web build (`guptrix.netlify.app`) inside an Android WebView. This caused slow initialization, poor touch responsiveness, broken offline states, and a generic wrapped web appearance.

The goal is to rebuild Guptrix as a 100% native Android application using Kotlin and Jetpack Compose. The rebuilt app will feature a local-first, offline-capable architecture, hardware-backed biometric security, end-to-end client-side encryption (AES-256-GCM), a distraction-free note editor with an integrated drawing canvas, and a modern, premium Material 3 design without visual clutter.

---

## 2. Goals, Non-Goals, and Anti-Goals

### Goals
- **Full Native Performance:** Instant app startup, smooth 120Hz scrolling, native gesture navigation, and low memory footprint.
- **Local-First Reliability:** The app works fully offline immediately without requiring account creation. Notes are stored locally in an encrypted Room SQLite database.
- **End-to-End Cryptography:** Client-side encryption for all note data (titles, text content, drawing canvas data) using AES-256-GCM and PBKDF2-HMAC-SHA256 key derivation.
- **Hardware-Backed Biometrics:** Fast, secure unlocking via Android BiometricPrompt (fingerprint or face unlock) backed by the Android Keystore.
- **Modern Premium UI/UX:** A bespoke, restrained design system free of generic AI slop: deep dark mode palette, crisp typographic hierarchy, tactile micro-interactions, and 1dp subtle border elevation instead of heavy blurred shadows.
- **Optional Backend Sync:** Clean integration with the existing Guptrix Express/Node.js backend API (`/api/v1/auth`, `/api/v1/note`) for users who want multi-device synchronization.

### Non-Goals
- Web-view fallbacks: No WebViews or hybrid bridges will remain in the codebase.
- Complex third-party document viewers: Office documents or proprietary spreadsheet rendering are out of scope.
- Multiplatform builds: Focus exclusively on Android native performance without cross-platform overhead.

### Anti-Goals
- Mandatory login walls: The user must never be blocked from writing notes by a forced cloud login screen.
- Plaintext persistence: Under no circumstances should unencrypted note content or master passphrases be written to disk or logs.
- Cluttered interface: Avoid nested cards, noisy gradients, emoji icons, or redundant status chips.

---

## 3. Constraints

- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (BOM 2024.09.00 or newer)
- **Minimum SDK:** API 26 (Android 8.0 Oreo) to support modern crypto and Keystore features
- **Target SDK:** API 35 (Android 15)
- **Local Storage:** Room Database 2.6+ with SQLCipher or payload-level AES-256-GCM encryption
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`)

---

## 4. Approaches Considered

### Approach 1: Jetpack Compose with Material 3 (Selected)
- **Description:** Rebuild all UI layers using declarative Jetpack Compose components, eliminating XML layouts, Fragment transactions, and WebViewManager.
- **Pros:** 
  - Fluid declarative state handling with Kotlin Flows and StateFlow.
  - Native gesture animations and shared-element transitions.
  - Custom drawing canvas implemented natively in a few dozen lines via Compose Canvas API.
  - Consistent tokenized theming across light and dark modes.
- **Cons:** Requires adding Compose compiler and UI dependencies to `build.gradle.kts`.
- **Verdict:** Selected. Delivers the modern, premium native experience requested by the user.

### Approach 2: Traditional Android Views (XML + ViewBinding + RecyclerView)
- **Description:** Keep the existing XML layout system, but replace `WebViewManager` with native RecyclerViews and XML components.
- **Pros:** Zero new build tool dependencies; keeps current project structure intact.
- **Cons:** High boilerplate code for list adapters, fragment backstacks, view bindings, and canvas interactions. Slower to iterate and harder to achieve fluid modern micro-interactions.
- **Verdict:** Rejected.

---

## 5. Architectural Design

```
+-----------------------------------------------------------+
|                   Jetpack Compose UI Layer                |
|  - VaultLockScreen     - NotesGridScreen   - EditorScreen |
|  - CanvasDrawingScreen - TrashScreen       - SettingsScreen|
+-----------------------------+-----------------------------+
                              | (StateFlow / Events)
+-----------------------------v-----------------------------+
|                     ViewModel Layer                       |
|  - VaultViewModel      - NotesViewModel    - SyncViewModel|
+--------------+------------------------------+--------------+
               |                              |
+--------------v---------------+ +------------v-------------+
|    Crypto & Security Core    | |    Repository & Storage   |
| - AesGcmCipher               | | - NoteRepository          |
| - Pbkdf2KeyDerivation        | | - Room AppDatabase        |
| - BiometricKeyStoreManager   | | - Encrypted Preferences   |
+------------------------------+ +------------+--------------+
                                              | (Optional Sync)
                                 +------------v--------------+
                                 |    Network Client (Ktor)  |
                                 | - Guptrix REST API Client |
                                 +---------------------------+
```

### 5.1 Cryptography Specification

1. **Master Key Derivation (KEK):**
   - Algorithm: PBKDF2 with HMAC-SHA256
   - Iterations: 100,000 rounds
   - Salt: 16 cryptographically random bytes generated via `SecureRandom` and stored in encrypted app preferences.
   - Key Length: 256 bits

2. **Note Payload Encryption (DEK):**
   - Cipher: `AES/GCM/NoPadding`
   - Key Size: 256-bit AES
   - IV (Initialization Vector): 12 cryptographically random bytes generated per field (`titleIV`, `contentIV`, `canvasIV`).
   - Authentication Tag: 128-bit GCM tag appended automatically to the ciphertext.
   - Format: Base64-encoded ciphertext and Base64-encoded IV, directly compatible with the existing `Guptrix_BE` note model.

3. **Biometric Integration:**
   - Keys stored inside the Android hardware-backed Keystore (`AndroidKeyStore`).
   - `KeyGenParameterSpec` configured with `PURPOSE_ENCRYPT or PURPOSE_DECRYPT`, requiring user authentication via `BiometricPrompt.CryptoObject`.

---

### 5.2 Data Model & Room Schema

```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val noteId: String,               // UUID or backend ID
    val titleCipher: String,           // Base64 encrypted title
    val titleIV: String,               // Base64 IV
    val contentCipher: String?,        // Base64 encrypted body text
    val contentIV: String?,            // Base64 IV
    val canvasCipher: String?,         // Base64 encrypted stroke points
    val canvasIV: String?,             // Base64 IV
    val color: String = "default",     // default, yellow, green, blue, pink, purple, orange
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,    // Soft-delete trash flag
    val deletedAt: Long? = null,       // Timestamp for 15-day TTL
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY // LOCAL_ONLY, SYNCED, PENDING_UPLOAD
)
```

---

### 5.3 User Interface & Hallmark Design Tokens

1. **Color Palette (Muted Obsidian Theme):**
   - Background: `#101216` (Deep Slate Charcoal, non-pure black to avoid harsh OLED smearing)
   - Surface Elevated: `#181B20` (Subtle 1dp border `#282D37`)
   - Surface Card: `#1E222A`
   - Primary Accent: `#3B82F6` (Electric Cobalt) or `#10B981` (Emerald signal)
   - Text Primary: `#F3F4F6`
   - Text Secondary: `#9CA3AF`
   - Destructive / Alert: `#EF4444`

2. **Typography System:**
   - Headline: Inter / System Sans, SemiBold, clean tracking (0sp).
   - Body: Clean readable regular face (15sp, 1.4 line height).
   - Labels: Untracked sentence case (no small caps, no forced uppercase).

3. **Screen Layouts:**
   - **Vault Lock Screen:** Clean numeric PIN entry or one-tap fingerprint icon. Zero clutter, no distracting branding banners.
   - **Notes Grid Screen:** Staggered two-column grid. Notes show title, short preview snippet (decrypted on the fly into memory), pin indicator, and date. Top bar features a minimalist search field and filter tabs (All, Pinned, Trash).
   - **Note Editor Screen:** Fullscreen minimalist canvas. Top bar has back navigation, color chip selector, pin toggle, drawing mode toggle, and delete. Body contains seamless title input and expanding content field.
   - **Canvas Drawing Mode:** Smooth finger-drawing overlay on top of note, capturing stroke paths as compact JSON vectors before encrypting.
   - **Trash Screen:** List of deleted notes with remaining days indicator, one-tap restore, and permanent purge.
   - **Settings Screen:** Biometric toggle, export encrypted JSON backup, server connection settings, and app version details.

---

## 6. Implementation Stages

1. **Dependencies & Gradle Build Setup:**
   - Add Jetpack Compose BOM, Material 3, Navigation Compose, and Lifecycle ViewModel Compose to `android-app/app/build.gradle.kts`.
   - Update `MainActivity.kt` to call `setContent { GuptrixApp() }`.
2. **Crypto & Storage Core:**
   - Implement `CryptoManager.kt` handling AES-256-GCM and PBKDF2 derivation.
   - Update Room `AppDatabase` and `NoteDao` to support modern CRUD, search queries, and trash filtering.
3. **UI Screens & Navigation:**
   - Implement theme tokens and typography in `ui/theme/`.
   - Build `NotesGridScreen`, `NoteEditorScreen`, `CanvasView`, `VaultLockScreen`, and `TrashScreen`.
4. **Purge Obsolete Code:**
   - Delete `WebViewManager.kt`, legacy XML layout files, and unused stub fragments.
5. **Verification & Testing:**
   - Verify local compilation with `./gradlew assembleDebug`.
   - Run unit tests for encryption/decryption round-trips to verify data integrity.

---

## 7. What is Left to the Builder

- Choice between Ktor client and Retrofit for the optional backend synchronization module.
- Exact stroke smoothing algorithm (Bézier curves vs quadTo) for the Compose drawing canvas.
- Animation specs (spring physics vs tween duration) for grid card opening transitions.

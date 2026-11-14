package com.guptrix.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.guptrix.app.crypto.KeystoreVaultManager
import com.guptrix.app.repository.NoteRepository
import com.guptrix.app.ui.dashboard.NotesDashboardScreen
import com.guptrix.app.ui.dashboard.NotesViewModel
import com.guptrix.app.ui.editor.EditorViewModel
import com.guptrix.app.ui.editor.NoteEditorScreen
import com.guptrix.app.ui.settings.SettingsScreen
import com.guptrix.app.ui.settings.SettingsViewModel
import com.guptrix.app.ui.trash.TrashScreen
import com.guptrix.app.ui.trash.TrashViewModel
import com.guptrix.app.ui.vault.VaultLockScreen
import com.guptrix.app.ui.vault.VaultViewModel

@Composable
fun GuptrixNavHost(
    repository: NoteRepository,
    vaultManager: KeystoreVaultManager,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Dashboard.route,
    onTriggerBiometrics: (() -> Unit)? = null
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Notes Dashboard
        composable(Screen.Dashboard.route) {
            val notesViewModel = remember { NotesViewModel(repository) }
            NotesDashboardScreen(
                viewModel = notesViewModel,
                onNoteClick = { noteId ->
                    navController.navigate(Screen.Editor.createRoute(noteId))
                },
                onNewNoteClick = {
                    navController.navigate(Screen.Editor.createRoute(null))
                },
                onVaultClick = {
                    navController.navigate(Screen.Vault.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onTrashClick = {
                    navController.navigate(Screen.Trash.route)
                }
            )
        }

        // Note Editor
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString("noteId")
            val noteId = if (noteIdArg == "new") null else noteIdArg
            val editorViewModel = remember(noteId) { EditorViewModel(repository) }
            NoteEditorScreen(
                noteId = noteId,
                viewModel = editorViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Vault Lock Screen
        composable(Screen.Vault.route) {
            val vaultViewModel = remember { VaultViewModel(vaultManager) }
            VaultLockScreen(
                viewModel = vaultViewModel,
                onUnlocked = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onTriggerBiometrics = onTriggerBiometrics
            )
        }

        // Trash Screen
        composable(Screen.Trash.route) {
            val trashViewModel = remember { TrashViewModel(repository) }
            TrashScreen(
                viewModel = trashViewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(Screen.Settings.route) {
            val settingsViewModel = remember { SettingsViewModel(repository, vaultManager) }
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToVault = {
                    navController.navigate(Screen.Vault.route)
                }
            )
        }
    }
}

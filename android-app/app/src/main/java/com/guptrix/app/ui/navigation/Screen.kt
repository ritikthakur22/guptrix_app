package com.guptrix.app.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Editor : Screen("editor/{noteId}") {
        fun createRoute(noteId: String?): String = "editor/${noteId ?: "new"}"
    }
    data object Vault : Screen("vault")
    data object Trash : Screen("trash")
    data object Settings : Screen("settings")
}

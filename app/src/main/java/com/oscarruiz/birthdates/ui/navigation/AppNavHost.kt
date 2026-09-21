package com.oscarruiz.birthdates.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oscarruiz.birthdates.AppContainer
import com.oscarruiz.birthdates.domain.model.AppSettings
import com.oscarruiz.birthdates.ui.backup.BackupRoute
import com.oscarruiz.birthdates.ui.edit.EditRoute
import com.oscarruiz.birthdates.ui.home.HomeRoute
import com.oscarruiz.birthdates.ui.pro.ProRoute
import com.oscarruiz.birthdates.ui.settings.SettingsRoute

object Routes {
    const val HOME = "home"
    const val EDIT = "edit?id={id}"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val PRO = "pro"

    fun edit(id: String?) = if (id == null) "edit" else "edit?id=$id"
}

@Composable
fun AppNavHost(container: AppContainer, settings: AppSettings, onStartAds: () -> Unit) {
    val nav = rememberNavController()
    val adsReady by container.adsController.adsReady.collectAsStateWithLifecycle()
    val showAds = adsReady && !settings.isPro

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeRoute(
                container = container,
                settings = settings,
                showAds = showAds,
                onAdd = { nav.navigate(Routes.edit(null)) },
                onEdit = { id -> nav.navigate(Routes.edit(id)) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
                onPro = { nav.navigate(Routes.PRO) },
                onRestore = { nav.navigate(Routes.BACKUP) },
                onStartAds = onStartAds,
            )
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            EditRoute(
                container = container,
                id = entry.arguments?.getString("id"),
                isPro = settings.isPro,
                onDone = { nav.popBackStack() },
                onPro = { nav.navigate(Routes.PRO) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                container = container,
                settings = settings,
                onBack = { nav.popBackStack() },
                onPro = { nav.navigate(Routes.PRO) },
                onBackup = { nav.navigate(Routes.BACKUP) },
            )
        }
        composable(Routes.BACKUP) {
            BackupRoute(container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.PRO) {
            ProRoute(container = container, isPro = settings.isPro, onBack = { nav.popBackStack() })
        }
    }
}

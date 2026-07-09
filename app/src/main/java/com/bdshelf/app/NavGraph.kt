package com.bdshelf.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bdshelf.app.ui.albumform.AlbumFormScreen
import com.bdshelf.app.ui.home.HomeScreen
import com.bdshelf.app.ui.onboarding.OnboardingScreen
import com.bdshelf.app.ui.releases.ReleasesScreen
import com.bdshelf.app.ui.scanner.ScannerScreen
import com.bdshelf.app.ui.series.SeriesListScreen
import com.bdshelf.app.ui.seriesdetail.SeriesDetailScreen
import com.bdshelf.app.ui.seriesform.SeriesFormScreen
import com.bdshelf.app.ui.settings.SettingsScreen
import com.bdshelf.app.ui.verdict.VerdictScreen

/** Graphe de navigation (§NAVIGATION ROUTES). Profondeur maximale : 2 niveaux. */
@Composable
fun BdShelfNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onImportComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onScanClick = { navController.navigate(Routes.SCANNER) },
                onCollectionClick = { navController.navigate(Routes.SERIES_LIST) },
                onReleasesClick = { navController.navigate(Routes.RELEASES) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SCANNER) {
            ScannerScreen(
                onBarcodeScanned = { ean ->
                    navController.navigate(Routes.verdict(ean)) {
                        popUpTo(Routes.SCANNER) { inclusive = true }
                    }
                },
                onManualEntry = { navController.navigate(Routes.SERIES_LIST) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.VERDICT,
            arguments = listOf(navArgument("ean") { type = NavType.StringType }),
        ) { backStackEntry ->
            val ean = backStackEntry.arguments?.getString("ean") ?: return@composable
            VerdictScreen(
                ean = ean,
                onBackToHome = { navController.popBackStack(Routes.HOME, inclusive = false) },
                onCreateAlbum = { seriesId, scannedEan, tomeNumber ->
                    navController.navigate(Routes.albumForm(seriesId, ean = scannedEan, tomeNumber = tomeNumber))
                },
                onCreateNewSeries = { suggestedTitle, scannedEan, tomeNumber ->
                    navController.navigate(Routes.seriesForm(title = suggestedTitle, ean = scannedEan, tomeNumber = tomeNumber))
                },
            )
        }

        composable(Routes.SERIES_LIST) {
            SeriesListScreen(
                onBack = { navController.popBackStack() },
                onSeriesClick = { seriesId -> navController.navigate(Routes.seriesDetail(seriesId)) },
                onAddSeries = { navController.navigate(Routes.seriesForm()) },
            )
        }

        composable(
            route = Routes.SERIES_DETAIL,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            SeriesDetailScreen(
                seriesId = seriesId,
                onBack = { navController.popBackStack() },
                onEditSeries = { id -> navController.navigate(Routes.seriesForm(id)) },
                onAlbumClick = { albumId -> navController.navigate(Routes.albumForm(seriesId, albumId = albumId)) },
                onAddTome = { id -> navController.navigate(Routes.albumForm(id)) },
                onGapClick = { id, tomeNumber -> navController.navigate(Routes.albumForm(id, tomeNumber = tomeNumber)) },
            )
        }

        composable(
            route = Routes.ALBUM_FORM,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument(Routes.ALBUM_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.TOME_NUMBER_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.EAN_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            val albumId = backStackEntry.arguments?.getString(Routes.ALBUM_ID_ARG)
            val tomeNumber = backStackEntry.arguments?.getString(Routes.TOME_NUMBER_ARG)?.toIntOrNull()
            val ean = backStackEntry.arguments?.getString(Routes.EAN_ARG)
            AlbumFormScreen(
                seriesId = seriesId,
                albumId = albumId,
                prefilledTomeNumber = tomeNumber,
                prefilledEan = ean,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.SERIES_FORM,
            arguments = listOf(
                navArgument("seriesId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.TITLE_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.TOME_NUMBER_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument(Routes.EAN_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId")
            val prefilledTitle = backStackEntry.arguments?.getString(Routes.TITLE_ARG)
            val tomeNumber = backStackEntry.arguments?.getString(Routes.TOME_NUMBER_ARG)?.toIntOrNull()
            val ean = backStackEntry.arguments?.getString(Routes.EAN_ARG)
            SeriesFormScreen(
                seriesId = seriesId,
                prefilledTitle = prefilledTitle,
                onBack = { navController.popBackStack() },
                onSaved = { createdSeriesId ->
                    // Série créée depuis une suggestion de scan (§6.4) : enchaîne directement
                    // sur la fiche du tome plutôt que de revenir en arrière, en retirant le
                    // formulaire série (déjà enregistré) de la pile.
                    if (createdSeriesId != null && (tomeNumber != null || ean != null)) {
                        navController.navigate(Routes.albumForm(createdSeriesId, tomeNumber = tomeNumber, ean = ean)) {
                            popUpTo(Routes.SERIES_FORM) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onDeleted = { navController.popBackStack(Routes.SERIES_LIST, inclusive = false) },
            )
        }

        composable(Routes.RELEASES) {
            ReleasesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

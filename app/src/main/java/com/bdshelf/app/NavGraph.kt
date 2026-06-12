package com.bdshelf.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bdshelf.app.ui.albumform.AlbumFormScreen
import com.bdshelf.app.ui.home.HomeScreen
import com.bdshelf.app.ui.onboarding.OnboardingScreen
import com.bdshelf.app.ui.series.SeriesListScreen
import com.bdshelf.app.ui.seriesdetail.SeriesDetailScreen
import com.bdshelf.app.ui.seriesform.SeriesFormScreen

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
            Box(modifier = Modifier.fillMaxSize()) { Text("TODO: Scanner") }
        }

        composable(
            route = Routes.VERDICT,
            arguments = listOf(navArgument("ean") { type = NavType.StringType }),
        ) {
            Box(modifier = Modifier.fillMaxSize()) { Text("TODO: Verdict") }
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
            ),
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: return@composable
            val albumId = backStackEntry.arguments?.getString(Routes.ALBUM_ID_ARG)
            val tomeNumber = backStackEntry.arguments?.getString(Routes.TOME_NUMBER_ARG)?.toIntOrNull()
            AlbumFormScreen(
                seriesId = seriesId,
                albumId = albumId,
                prefilledTomeNumber = tomeNumber,
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
            ),
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId")
            SeriesFormScreen(
                seriesId = seriesId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack(Routes.SERIES_LIST, inclusive = false) },
            )
        }

        composable(Routes.RELEASES) {
            Box(modifier = Modifier.fillMaxSize()) { Text("TODO: Releases") }
        }

        composable(Routes.SETTINGS) {
            Box(modifier = Modifier.fillMaxSize()) { Text("TODO: Settings") }
        }
    }
}

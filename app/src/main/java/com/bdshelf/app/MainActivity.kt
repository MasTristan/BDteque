package com.bdshelf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.bdshelf.app.data.prefs.ThemeMode
import com.bdshelf.app.ui.theme.BdShelfTheme
import com.bdshelf.app.ui.theme.LocalReduceMotion
import com.bdshelf.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.flow.map

/** Activité unique (§ARCHITECTURE). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BdShelfApplication
        val openReleases = intent?.getBooleanExtra(EXTRA_OPEN_RELEASES, false) ?: false
        // Consommé une fois : évite de re-naviguer vers « À paraître » à chaque
        // recréation d'activité (rotation, retour depuis l'arrière-plan).
        intent?.removeExtra(EXTRA_OPEN_RELEASES)

        setContent {
            val themeMode by app.userPreferencesRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Icônes de la barre d'état accordées au thème effectif : le XML
            // (values/values-night) ne couvre que le mode "Système".
            LaunchedEffect(darkTheme) {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !darkTheme
            }
            BdShelfTheme(darkTheme = darkTheme) {
                val reduceMotion = rememberReduceMotion()
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val seedImported by remember {
                            app.userPreferencesRepository.seedImported.map<Boolean, Boolean?> { it }
                        }.collectAsStateWithLifecycle(initialValue = null)

                        seedImported?.let { imported ->
                            val navController = rememberNavController()
                            BdShelfNavGraph(
                                navController = navController,
                                startDestination = if (imported) Routes.HOME else Routes.ONBOARDING,
                            )

                            // Tap sur une notification de sortie → écran « À paraître » (§7).
                            if (openReleases) {
                                LaunchedEffect(Unit) { navController.navigate(Routes.RELEASES) }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_RELEASES = "open_releases"
    }
}

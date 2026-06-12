package com.bdshelf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bdshelf.app.ui.theme.BdShelfTheme
import com.bdshelf.app.ui.theme.LocalReduceMotion
import com.bdshelf.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.flow.map

/** Activité unique (§ARCHITECTURE). */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BdShelfApplication

        setContent {
            BdShelfTheme {
                val reduceMotion = rememberReduceMotion()
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        val seedImported by remember {
                            app.userPreferencesRepository.seedImported.map<Boolean, Boolean?> { it }
                        }.collectAsStateWithLifecycle(initialValue = null)

                        seedImported?.let { imported ->
                            BdShelfNavGraph(
                                startDestination = if (imported) Routes.HOME else Routes.ONBOARDING,
                            )
                        }
                    }
                }
            }
        }
    }
}

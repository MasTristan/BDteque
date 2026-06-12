package com.bdshelf.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bdshelf.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.launch

/**
 * État d'animation "tampon encreur" (§5.5 / §ANIMATION).
 *
 * - `scale` : pulsation 1.0 -> 1.15 -> 1.0 sur 300ms (FastOutSlowIn) au passage
 *   `owned = false -> true`.
 * - `ownedProgress` : 0 (manquant) -> 1 (possédé), anime simultanément le
 *   remplissage de la tranche et la disparition de la bordure pointillée.
 *
 * Si `LocalReduceMotion` est actif, l'état final est appliqué sans transition.
 */
class StampAnimationState internal constructor(
    val scale: Animatable<Float, AnimationVector1D>,
    val ownedProgress: Animatable<Float, AnimationVector1D>,
)

@Composable
fun rememberStampAnimation(owned: Boolean): StampAnimationState {
    val reduceMotion = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    val ownedProgress = remember { Animatable(if (owned) 1f else 0f) }
    var previousOwned by remember { mutableStateOf(owned) }

    LaunchedEffect(owned) {
        if (owned != previousOwned) {
            when {
                reduceMotion -> {
                    ownedProgress.snapTo(if (owned) 1f else 0f)
                    scale.snapTo(1f)
                }
                owned -> {
                    launch {
                        scale.animateTo(1.15f, tween(150, easing = FastOutSlowInEasing))
                        scale.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
                    }
                    ownedProgress.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
                }
                else -> ownedProgress.snapTo(0f)
            }
            previousOwned = owned
        }
    }

    return remember(scale, ownedProgress) { StampAnimationState(scale, ownedProgress) }
}

package com.bdshelf.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bdshelf.app.R
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.ui.theme.Ink
import com.bdshelf.app.ui.theme.Surface

private val TileWidth = 52.dp
private val CornerRadiusDp = 6.dp

/**
 * Une tranche de livre (§5.4).
 *
 * - Possédé : fond `seriesColor`, numéro Fraunces en clair.
 * - Manquant : fond papier (thème), bordure pointillée fantôme, numéro estompé.
 * - Marqueur bas : point plein = lu, anneau = prêté, rien = non lu.
 *
 * Les couleurs de tranche restant saturées dans les deux thèmes, le numéro et
 * le marqueur d'un tome possédé gardent leurs encres fixes ([Surface], [Ink]) ;
 * le reste suit le ColorScheme pour s'adapter au mode sombre.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpineTile(
    tomeNumber: Int?,
    seriesColor: Color,
    owned: Boolean,
    readStatus: ReadStatus,
    height: Dp,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val anim = rememberStampAnimation(owned)
    val progress = anim.ownedProgress.value

    val backgroundColor = lerp(MaterialTheme.colorScheme.background, seriesColor, progress)
    val numberColor = lerp(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), Surface, progress)
    val borderAlpha = 1f - progress
    val ghostColor = MaterialTheme.colorScheme.outline
    val highlightColor = MaterialTheme.colorScheme.primary

    val description = spineContentDescription(tomeNumber, owned, readStatus, highlighted)

    Box(
        modifier = modifier
            .size(width = TileWidth, height = height)
            .graphicsLayer {
                scaleX = anim.scale.value
                scaleY = anim.scale.value
            }
            .clip(RoundedCornerShape(CornerRadiusDp))
            .background(backgroundColor)
            .dashedGhostBorder(color = ghostColor, alpha = borderAlpha, cornerRadius = CornerRadiusDp)
            .then(
                if (highlighted) Modifier.highlightBorder(color = highlightColor, cornerRadius = CornerRadiusDp) else Modifier,
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics {
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tomeNumber?.toString() ?: stringResource(R.string.spine_unnumbered_marker),
            style = MaterialTheme.typography.titleMedium,
            color = numberColor,
        )
        ReadStatusMarker(
            readStatus = readStatus,
            tint = Ink,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ReadStatusMarker(readStatus: ReadStatus, tint: Color, modifier: Modifier = Modifier) {
    if (readStatus == ReadStatus.UNREAD) return
    Box(
        modifier = modifier
            .padding(bottom = 10.dp)
            .size(10.dp)
            .drawWithContent {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                when (readStatus) {
                    ReadStatus.READ -> drawCircle(color = tint, radius = radius, center = center)
                    ReadStatus.LENT -> drawCircle(
                        color = tint,
                        radius = radius - 1.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                    ReadStatus.UNREAD -> Unit
                }
            },
    )
}

private fun Modifier.dashedGhostBorder(color: Color, alpha: Float, cornerRadius: Dp): Modifier =
    if (alpha <= 0f) this else drawWithContent {
        drawContent()
        drawRoundRect(
            color = color.copy(alpha = alpha),
            style = Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
        )
    }

private fun Modifier.highlightBorder(color: Color, cornerRadius: Dp): Modifier = drawWithContent {
    drawContent()
    drawRoundRect(
        color = color,
        style = Stroke(width = 3.dp.toPx()),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
    )
}

@Composable
private fun spineContentDescription(tomeNumber: Int?, owned: Boolean, readStatus: ReadStatus, highlighted: Boolean): String {
    val base = if (tomeNumber != null) {
        stringResource(if (owned) R.string.spine_cd_owned else R.string.spine_cd_missing, tomeNumber)
    } else {
        stringResource(if (owned) R.string.spine_cd_unnumbered_owned else R.string.spine_cd_unnumbered_missing)
    }
    val suffix = when (readStatus) {
        ReadStatus.READ -> stringResource(R.string.spine_cd_read_suffix)
        ReadStatus.LENT -> stringResource(R.string.spine_cd_lent_suffix)
        ReadStatus.UNREAD -> ""
    }
    val highlightSuffix = if (highlighted) stringResource(R.string.spine_cd_highlighted_suffix) else ""
    return base + suffix + highlightSuffix
}

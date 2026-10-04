package com.mangotv.app.ui.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.haptics.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.RowStyle
import com.mangotv.app.ui.components.LocalCardActionsMenu
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.ProgressFill
import com.mangotv.app.ui.theme.ProgressTrack
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import com.mangotv.app.ui.theme.TextTertiary
import com.mangotv.app.ui.theme.WatchedGreen

/**
 * A poster for touch: tap opens the title, press and hold opens the quick-actions menu (with a small buzz). No focus ring, no zoom.
 * [width] defaults to the window's poster width; a grid passes the width of its column so posters fill it exactly.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobilePoster(
    content: Content,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RowStyle = RowStyle.STANDARD,
    width: Dp? = null,
    fillWidth: Boolean = false
) {
    val wide = style == RowStyle.CONTINUE_WATCHING
    val cardWidth = width ?: if (wide) MangoDimens.ContinueWatchingWidth else MangoDimens.PosterWidth
    val aspect = if (wide) 16f / 9f else 2f / 3f
    val menu = LocalCardActionsMenu.current
    val haptics = LocalHapticFeedback.current
    val imageUrl = if (wide) content.backdropUrl else content.posterUrl

    Column(modifier = if (fillWidth) modifier.fillMaxWidth() else modifier.width(cardWidth)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(MangoDimens.CardCornerRadius))
                .background(MangoSurface)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        menu.open(content)
                    }
                )
        ) {
            AsyncImage(
                model = rememberOpaqueImageRequest(imageUrl),
                contentDescription = content.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (content.watched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .background(WatchedGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Watched", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            if (wide) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                )
                content.watchProgress?.let { progress ->
                    Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(ProgressTrack)) {
                        Box(Modifier.fillMaxWidth(progress.fraction).height(4.dp).background(ProgressFill))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = content.title,
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val sub = when {
            wide -> content.watchProgress?.let { p ->
                if (p.seasonNumber != null && p.episodeNumber != null) "S${p.seasonNumber} E${p.episodeNumber}" else null
            }
            content.recommendReason != null -> content.recommendReason
            else -> content.year?.toString()
        }
        if (sub != null) {
            Text(sub, color = TextTertiary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

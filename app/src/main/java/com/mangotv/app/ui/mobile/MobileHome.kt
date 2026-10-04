package com.mangotv.app.ui.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mangotv.app.data.model.Content
import com.mangotv.app.data.model.HomeSection
import com.mangotv.app.data.model.RowStyle
import com.mangotv.app.ui.components.rememberOpaqueImageRequest
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** Home for touch: a swipeable banner of featured titles, then rows that scroll sideways with a finger. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MobileHomeContent(
    heroItems: List<Content>,
    sections: List<HomeSection>,
    savedIds: Set<String>,
    onOpen: (Content) -> Unit,
    onResume: (Content) -> Unit,
    onPlay: (Content) -> Unit,
    onToggleMyList: (Content) -> Unit,
    onTopBarClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 } }
    Box(modifier.fillMaxSize().background(MangoBackground)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (heroItems.isNotEmpty()) {
                item(key = "hero") {
                    MobileHero(heroItems, savedIds, onOpen, onPlay, onToggleMyList)
                }
            } else {
                item(key = "hero_gap") { Spacer(Modifier.height(MangoDimens.NavBarHeight)) }
            }
            items(sections, key = { it.id }) { section ->
                Column(Modifier.padding(top = MangoDimens.RowSpacing)) {
                    Text(
                        text = section.title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = MangoDimens.ScreenPaddingHorizontal, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = MangoDimens.ScreenPaddingHorizontal),
                        horizontalArrangement = Arrangement.spacedBy(MangoDimens.CardSpacing)
                    ) {
                        items(section.items, key = { it.id }) { item ->
                            MobilePoster(content = item, style = section.style, onClick = { if (section.style == RowStyle.CONTINUE_WATCHING) onResume(item) else onOpen(item) })
                        }
                    }
                }
            }
            item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
        }
        TopNavBar(
            transparentBackground = !scrolled,
            modifier = Modifier.align(Alignment.TopCenter),
            onItemClick = onTopBarClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MobileHero(
    items: List<Content>,
    savedIds: Set<String>,
    onOpen: (Content) -> Unit,
    onPlay: (Content) -> Unit,
    onToggleMyList: (Content) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    // Moves on by itself, but never fights a finger: the timer restarts after every swipe.
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress, items.size) {
        if (items.size > 1 && !pagerState.isScrollInProgress) {
            delay(7000)
            pagerState.animateScrollToPage((pagerState.settledPage + 1) % items.size)
        }
    }
    val compact = MobileMetrics.isCompact
    Box(Modifier.fillMaxWidth().aspectRatio(if (compact) 0.82f else 1.9f)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = items[page]
            val savedNow = item.id in savedIds
            Box(Modifier.fillMaxSize().clickable { onOpen(item) }) {
                AsyncImage(
                    model = rememberOpaqueImageRequest(if (compact) item.posterUrl ?: item.backdropUrl else item.backdropUrl ?: item.posterUrl),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to MangoBackground
                        )
                    )
                )
                Column(
                    modifier = Modifier
                        .align(if (compact) Alignment.BottomCenter else Alignment.BottomStart)
                        .padding(horizontal = MangoDimens.ScreenPaddingHorizontal)
                        .padding(bottom = 28.dp),
                    horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    if (item.logoUrl != null) {
                        AsyncImage(
                            model = rememberOpaqueImageRequest(item.logoUrl),
                            contentDescription = item.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(if (compact) 64.dp else 80.dp).widthIn(max = 280.dp)
                        )
                    } else {
                        Text(
                            item.title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val meta = listOfNotNull(
                        item.year?.toString(),
                        item.genres.take(2).joinToString(" • ") { it.name }.takeIf { it.isNotEmpty() },
                        item.rating?.let { "★ ${"%.1f".format(it)}" }
                    ).joinToString("  ·  ")
                    if (meta.isNotEmpty()) {
                        Text(meta, color = TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 6.dp))
                    }
                    Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            modifier = Modifier
                                .height(MangoDimens.TouchTarget)
                                .clip(RoundedCornerShape(50))
                                .background(TextPrimary)
                                .clickable { onPlay(item) }
                                .padding(horizontal = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MangoBackground)
                            Text("Play", color = MangoBackground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(MangoDimens.TouchTarget)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                                .clickable { onToggleMyList(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (savedNow) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = if (savedNow) "Remove from My List" else "Add to My List",
                                tint = if (savedNow) ArcCyan else TextPrimary
                            )
                        }
                    }
                }
            }
        }
        // Page dots.
        if (items.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(items.size) { i ->
                    Box(
                        Modifier
                            .size(if (i == pagerState.currentPage) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(if (i == pagerState.currentPage) TextPrimary else Color(0x66FFFFFF))
                    )
                }
            }
        }
    }
}

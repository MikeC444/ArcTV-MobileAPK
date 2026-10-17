package com.mangotv.app.ui.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mangotv.app.ui.components.StreamingTile
import com.mangotv.app.ui.home.backdropWashColor
import kotlin.math.exp
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
    onOpenService: (Content) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 } }
    // The soft colour wash behind the hero (as on the web app): the colour of the title on show's artwork, glowing from the hero's lower edge onto the first
    // rows and fading between titles; none while the picture has no colour to take.
    var washTarget by remember { mutableStateOf<Color?>(null) }
    var lastWash by remember { mutableStateOf(Color.Transparent) }
    LaunchedEffect(washTarget) { washTarget?.let { lastWash = it } }
    val washColorNow by animateColorAsState(lastWash, animationSpec = tween(900), label = "heroWashColor")
    val washAlpha by animateFloatAsState(if (washTarget != null) 0.3f else 0f, animationSpec = tween(900), label = "heroWashAlpha")
    val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    Box(modifier.fillMaxSize().background(MangoBackground)) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (heroItems.isNotEmpty()) {
                item(key = "hero") {
                    // The hero is drawn first and the rows after it, so a light cast from the hero's lower edge lies behind the rows' pictures and text
                    Box(Modifier.drawWithContent {
                        drawContent()
                        drawHeroWash(washColorNow, washAlpha, screenHeightPx)
                    }) {
                        MobileHero(heroItems, savedIds, onOpen, onPlay, onToggleMyList, onBackdropColor = { washTarget = it })
                    }
                }
            } else {
                item(key = "hero_gap") { Spacer(Modifier.height(MangoDimens.NavBarHeight)) }
            }
            items(sections, key = { it.id }) { section ->
                if (section.style == RowStyle.STREAMING_SERVICES) {
                    StreamingTilesRow(section, onOpenService)
                    return@items
                }
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
    onToggleMyList: (Content) -> Unit,
    onBackdropColor: (Color?) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    // The colour of the title on show's artwork (null when it has none), for the wash Home casts onto the rows
    val context = LocalContext.current
    val current = items.getOrNull(pagerState.currentPage)
    LaunchedEffect(current?.backdropUrl) {
        val url = current?.backdropUrl
        onBackdropColor(if (url == null) null else backdropWashColor(context, url))
    }
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

/**
 * Home's "Streaming services" row (no heading above it, as on the web app): the service tiles side by side. A phone shows them at Continue Watching's card size
 * and scrolls them sideways; a tablet (a window at least 600dp wide) fits five across the row.
 */
@Composable
private fun StreamingTilesRow(section: HomeSection, onOpenService: (Content) -> Unit) {
    val pad = MangoDimens.ScreenPaddingHorizontal
    val gap = MangoDimens.CardSpacing
    val tileWidth = if (MobileMetrics.isCompact) MangoDimens.ContinueWatchingWidth else (MobileMetrics.widthDp.dp - pad * 2 - gap * 4) / 5
    LazyRow(
        modifier = Modifier.padding(top = MangoDimens.RowSpacing).semantics { contentDescription = "Streaming services" },
        // room above and below for the lift and the light beneath the tiles
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = pad, end = pad, top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        items(section.items, key = { it.id }) { tile ->
            StreamingTile(content = tile, width = tileWidth, onClick = { onOpenService(tile) })
        }
    }
}

/**
 * The hero's colour wash (the web app's `.home__wash`): one wide ellipse of [color] centred a little under the hero's lower edge, strongest in the middle and
 * fading smoothly to nothing at its rim (many eased stops, so there is no visible edge), drawn over the hero's bottom and the first rows' background. The rows
 * are drawn after it.
 */
private fun DrawScope.drawHeroWash(color: Color, alpha: Float, screenHeightPx: Float) {
    if (alpha <= 0.001f || color.alpha == 0f) return
    val top = size.height - screenHeightPx * 0.34f
    val height = screenHeightPx * 0.78f
    val centre = Offset(size.width / 2f, top + height / 2f)
    val radiusX = size.width * 0.8f
    val radiusY = height / 2f
    val stops = Array(9) { i ->
        val t = i / 8f
        t to color.copy(alpha = if (i == 8) 0f else alpha * exp(-3f * t * t))
    }
    scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = centre) {
        drawCircle(brush = Brush.radialGradient(colorStops = stops, center = centre, radius = radiusX), radius = radiusX, center = centre)
    }
}

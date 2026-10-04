package com.mangotv.app.ui.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mangotv.app.data.model.Content
import com.mangotv.app.ui.browse.BrowseHeaderFocus
import com.mangotv.app.ui.browse.CatalogSort
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

/**
 * Movies, TV Shows, Genre results and My List for touch: a poster grid that fills the width (the columns follow the screen size),
 * chip rows for filtering / sorting, and more titles loading in as you near the end.
 */
@Composable
fun MobileBrowseGrid(
    screenTitle: String,
    items: List<Content>,
    emptyMessage: String,
    onOpen: (Content) -> Unit,
    onLoadMore: () -> Unit,
    onTopBarClick: (String) -> Unit,
    filterOptions: List<String>,
    selectedFilterIndex: Int,
    onFilterSelected: (Int) -> Unit,
    headerAction: (@Composable (BrowseHeaderFocus) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var sort by remember { mutableStateOf(CatalogSort.FEATURED) }
    val sorted = remember(items, sort) {
        when (sort) {
            CatalogSort.FEATURED -> items
            CatalogSort.HIGHEST_RATED -> items.sortedByDescending { it.rating ?: -1.0 }
            CatalogSort.NEWEST -> items.sortedByDescending { it.year ?: -1 }
        }
    }
    val gridState = rememberLazyGridState()
    // Near the end of what is loaded: ask for the next page.
    LaunchedEffect(gridState, sorted.size) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (sorted.isNotEmpty() && last >= sorted.size - 8) onLoadMore() }
    }
    val noFocus = remember { BrowseHeaderFocus(FocusRequester(), FocusRequester(), null) }
    val pad = MangoDimens.ScreenPaddingHorizontal

    Box(modifier.fillMaxSize().background(MangoBackground)) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(MangoDimens.PosterWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = pad, end = pad, top = MangoDimens.NavBarHeight, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(MangoDimens.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(MangoDimens.RowSpacing)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(screenTitle, color = TextPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    if (headerAction != null) {
                        Spacer(Modifier.width(14.dp))
                        headerAction(noFocus)
                    }
                }
            }
            if (filterOptions.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow(filterOptions, selectedFilterIndex, onFilterSelected)
                }
            }
            // My List has its own "Sort by" drop-down beside the title, so the chips are only for the catalogue screens.
            if (filterOptions.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow(CatalogSort.entries.map { it.label }, sort.ordinal) { sort = CatalogSort.entries[it] }
                }
            }
            if (sorted.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        emptyMessage,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp)
                    )
                }
            }
            items(sorted, key = { "${it.providerId}:${it.id}" }) { item ->
                MobilePoster(content = item, fillWidth = true, onClick = { onOpen(item) })
            }
        }
        TopNavBar(
            transparentBackground = false,
            modifier = Modifier.align(Alignment.TopCenter),
            onItemClick = onTopBarClick
        )
    }
}

/** A row of tappable chips (one selected), scrolling sideways when they don't fit. */
@Composable
private fun ChipRow(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        items(labels.size) { index ->
            val on = index == selected
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) ArcCyan else Color(0x1FFFFFFF))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels[index],
                    color = if (on) MangoBackground else TextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

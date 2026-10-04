package com.mangotv.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mangotv.app.data.model.Content
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.navigation.routeForNavLabel
import com.mangotv.app.ui.components.EmptyState
import com.mangotv.app.ui.detail.PendingDetailCache
import com.mangotv.app.ui.home.TopNavBar
import com.mangotv.app.ui.mobile.MobilePoster
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.ErrorCoral
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoSurface
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

/** Search for touch: a normal text field (tap to type), recent searches to tap or swipe away with the X, and a poster grid of results. */
@Composable
fun SearchScreen(
    onNavigate: (String) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var fieldFocused by remember { mutableStateOf(false) }

    fun open(target: Content) {
        val providerId = target.providerId ?: return
        viewModel.rememberSearch(query)
        PendingDetailCache.stash(target)
        onNavigate(MangoRoutes.detail(providerId, target.type, target.id))
    }

    fun searchFor(term: String) {
        query = term
        viewModel.onQueryChanged(term)
        viewModel.submit(term)
        keyboard?.hide()
        focusManager.clearFocus()
    }

    val pad = MangoDimens.ScreenPaddingHorizontal
    Box(Modifier.fillMaxSize().background(MangoBackground)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(MangoDimens.PosterWidth),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = pad, end = pad, top = MangoDimens.NavBarHeight, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(MangoDimens.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(MangoDimens.RowSpacing)
        ) {
            item(key = "field", span = { GridItemSpan(maxLineSpan) }) {
                val shape = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(shape)
                        .background(MangoSurface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = if (fieldFocused) ArcCyan else TextSecondary, modifier = Modifier.padding(start = 16.dp).size(22.dp))
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.onQueryChanged(it)
                        },
                        placeholder = { Text("Search movies and TV shows") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.submit(query)
                            keyboard?.hide()
                            focusManager.clearFocus()
                        }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MangoSurface,
                            unfocusedContainerColor = MangoSurface,
                            focusedIndicatorColor = MangoSurface,
                            unfocusedIndicatorColor = MangoSurface,
                            cursorColor = ArcCyan
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { fieldFocused = it.isFocused }
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            Modifier.size(MangoDimens.TouchTarget).clickable {
                                query = ""
                                viewModel.onQueryChanged("")
                            },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = TextSecondary) }
                    }
                }
            }
            when (val state = uiState) {
                is SearchUiState.Idle -> {
                    if (recents.isEmpty()) {
                        item(key = "idle", span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Search for movies and TV shows across your installed addons.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        item(key = "recent_header", span = { GridItemSpan(maxLineSpan) }) {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent searches", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(
                                    "Clear",
                                    color = ArcCyan,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.clickable { viewModel.clearRecents() }.padding(horizontal = 8.dp, vertical = 12.dp)
                                )
                            }
                        }
                        items(recents, key = { "recent_$it" }, span = { GridItemSpan(maxLineSpan) }) { term ->
                            Row(
                                Modifier.fillMaxWidth().height(MangoDimens.TouchTarget).clickable { searchFor(term) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(end = 14.dp))
                                Text(term, color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), maxLines = 1)
                                Box(Modifier.size(MangoDimens.TouchTarget).clickable { viewModel.removeRecent(term) }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove $term", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                is SearchUiState.Searching -> item(key = "searching", span = { GridItemSpan(maxLineSpan) }) {
                    Text("Searching…", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 24.dp))
                }
                is SearchUiState.Results -> {
                    resultSection("Movies", state.movies, ::open)
                    resultSection("TV Shows", state.tvShows, ::open)
                    if (state.loading) {
                        item(key = "still", span = { GridItemSpan(maxLineSpan) }) {
                            Text("Still checking other addons…", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                is SearchUiState.NoResults -> item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "No results",
                        message = "Nothing found for \"${state.query}\". Try a different search."
                    )
                }
                is SearchUiState.Error -> item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    Text(state.message, color = ErrorCoral, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 24.dp))
                }
            }
        }
        TopNavBar(
            transparentBackground = false,
            modifier = Modifier.align(Alignment.TopCenter),
            onItemClick = { label -> routeForNavLabel(label)?.let(onNavigate) }
        )
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.resultSection(
    heading: String,
    results: List<Content>,
    onOpen: (Content) -> Unit
) {
    if (results.isEmpty()) return
    item(key = "heading_$heading", span = { GridItemSpan(maxLineSpan) }) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
            Text(heading, color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                if (results.size == 1) "1 result" else "${results.size} results",
                color = TextSecondary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
    items(results, key = { "${heading}_${it.providerId}:${it.id}" }) { item ->
        MobilePoster(content = item, fillWidth = true, onClick = { onOpen(item) })
    }
}

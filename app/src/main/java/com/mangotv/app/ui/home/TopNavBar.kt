package com.mangotv.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mangotv.app.MangoTvApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mangotv.app.navigation.PROFILES_NAV_LABEL
import com.mangotv.app.ui.components.ArcLogo
import com.mangotv.app.ui.components.TvFocusSurface
import com.mangotv.app.ui.profiles.ProfileAvatarTile
import com.mangotv.app.ui.theme.ArcBlue
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.ArcViolet
import com.mangotv.app.ui.theme.MangoBackground
import com.mangotv.app.ui.theme.MangoDimens
import com.mangotv.app.ui.theme.MangoMotion
import com.mangotv.app.ui.theme.TextPrimary
import com.mangotv.app.ui.theme.TextSecondary

// The frosted pill the links sit in, the profile chip beside it, and the wash behind the page you are on (same colours as the web app's top bar).
private val NavPillColor = Color(0x94141417)
private val NavPillBorder = Color(0x17FFFFFF)
private val NavSelectedWash = Color(0x24FFFFFF)
private val NavFocusWash = Color(0x17FFFFFF)

val MangoNavItems = listOf("Home", "Movies", "TV Shows", "Search", "My List", "Settings")

/** The nav items for someone without an account: the same tabs in the same places, with Settings replaced by Sign In. */
fun navItemsForGuest(items: List<String>): List<String> = items.map { if (it == "Settings") "Sign In" else it }

@Composable
fun TopNavBar(
    transparentBackground: Boolean,
    modifier: Modifier = Modifier,
    selectedIndex: Int = 0,
    selectedItemFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    onItemClick: (String) -> Unit = {},
    onNavigateDown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as MangoTvApplication).container }
    val isGuest by container.guestGate.isGuest.collectAsStateWithLifecycle()
    val profiles by container.profileRepository.state.collectAsStateWithLifecycle()
    val plus by container.plusRepository.status.collectAsStateWithLifecycle()
    val activeProfile = if (!isGuest && plus.active && profiles.supported) profiles.active else null
    val showSettings = !isGuest && activeProfile?.isKids != true

    val scrimAlpha by animateFloatAsState(
        targetValue = if (transparentBackground) 0f else 0.96f,
        animationSpec = tween(300),
        label = "navBarScrimAlpha"
    )
    // Under the status bar / camera cut-out; the screen below reserves MangoDimens.NavBarHeight for this bar.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to MangoBackground.copy(alpha = scrimAlpha),
                        0.75f to MangoBackground.copy(alpha = scrimAlpha),
                        1f to Color.Transparent
                    )
                )
            )
            .height(MangoDimens.NavBarHeight)
            .padding(start = MangoDimens.ScreenPaddingHorizontal, end = MangoDimens.ScreenPaddingHorizontal - 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { ArcLogo() }
        if (isGuest) {
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(NavSelectedWash)
                    .clickable { onItemClick("Sign In") }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Sign in", color = TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        } else {
            if (activeProfile != null) {
                Box(
                    modifier = Modifier
                        .size(MangoDimens.TouchTarget)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Switch profile") { onItemClick(PROFILES_NAV_LABEL) },
                    contentAlignment = Alignment.Center
                ) { ProfileAvatarTile(avatar = activeProfile.avatar, size = 32.dp, cornerRadius = 16.dp) }
            }
            if (showSettings) {
                Box(
                    modifier = Modifier
                        .size(MangoDimens.TouchTarget)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClickLabel = "Settings") { onItemClick("Settings") },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = TextPrimary) }
            }
        }
    }
}

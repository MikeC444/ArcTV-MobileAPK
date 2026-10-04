package com.mangotv.app.ui.mobile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mangotv.app.navigation.MangoRoutes
import com.mangotv.app.ui.components.ArcLogo
import com.mangotv.app.ui.theme.ArcCyan
import com.mangotv.app.ui.theme.MangoBackgroundElevated
import com.mangotv.app.ui.theme.TextSecondary

/** One place you can go from the bottom bar / side rail. */
data class MobileTab(val label: String, val route: String, val icon: ImageVector)

val PrimaryTabs = listOf(
    MobileTab("Home", MangoRoutes.HOME, Icons.Rounded.Home),
    MobileTab("Movies", MangoRoutes.MOVIES, Icons.Rounded.Movie),
    MobileTab("TV Shows", MangoRoutes.TV_SHOWS, Icons.Rounded.Tv),
    MobileTab("Search", MangoRoutes.SEARCH, Icons.Rounded.Search),
    MobileTab("My List", MangoRoutes.MY_LIST, Icons.Rounded.Bookmark)
)

/** The rail on wide windows has room for Settings too (the bottom bar keeps it in the top bar instead, to stay at five tabs). */
val RailTabs = PrimaryTabs + MobileTab("Settings", MangoRoutes.SETTINGS, Icons.Rounded.Settings)

/** Routes the bottom bar / rail is shown on (the main sections), as opposed to detail pages, sign-in and the player. */
val ShellRoutes = RailTabs.map { it.route }.toSet()

private val BarColor = MangoBackgroundElevated

@Composable
fun MobileBottomBar(currentRoute: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier = modifier, containerColor = BarColor, tonalElevation = 0.dp) {
        PrimaryTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ArcCyan,
                    selectedTextColor = ArcCyan,
                    indicatorColor = Color(0x1F19E6FF),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

@Composable
fun MobileRail(currentRoute: String?, showSettings: Boolean, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = BarColor
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            (if (showSettings) RailTabs else PrimaryTabs).forEach { tab ->
                NavigationRailItem(
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab.route) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.label, maxLines = 1) },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = ArcCyan,
                        selectedTextColor = ArcCyan,
                        indicatorColor = Color(0x1F19E6FF),
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    }
}

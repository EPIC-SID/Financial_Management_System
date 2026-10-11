package com.example.financeapp.ui.components

import androidx.compose.foundation.border
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.BackgroundWhite
import com.example.financeapp.ui.theme.BronzePrimary
import com.example.financeapp.ui.theme.SurfaceGray
import com.example.financeapp.ui.theme.TextGray

/**
 * Represents a tab in the bottom navigation bar.
 */
enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    TRANSACTIONS("Transactions", Icons.Filled.List),
    PROFILE("Profile", Icons.Filled.Person)
}

/**
 * Shared bottom navigation bar used across Home, Transactions, and Profile screens.
 * Extracted to avoid duplication and ensure consistent styling.
 */
@Composable
fun AppBottomBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit
) {
    NavigationBar(
        containerColor = BackgroundWhite,
        modifier = Modifier.border(1.dp, SurfaceGray, RectangleShape)
    ) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BronzePrimary,
                    selectedTextColor = BronzePrimary,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray,
                    indicatorColor = BackgroundWhite
                )
            )
        }
    }
}

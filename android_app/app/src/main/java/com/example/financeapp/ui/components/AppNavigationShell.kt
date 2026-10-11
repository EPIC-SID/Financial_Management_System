package com.example.financeapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.financeapp.R
import com.example.financeapp.ui.navigation.NavigationChoice
import com.example.financeapp.ui.navigation.Screen
import com.example.financeapp.ui.navigation.navigationChoices
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.OutlineNeutral
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.AppDataMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationShell(
    visible: Boolean,
    mode: AppDataMode,
    route: String?,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    if (!visible) {
        content(PaddingValues())
        return
    }
    val choices = navigationChoices(mode)
    val title = choices.firstOrNull { it.route == route }?.label ?: when (route) {
        Screen.EditProfile.route -> "Edit profile"
        Screen.Conflict.route -> "Adjust your goal"
        else -> "RupAI"
    }
    val nested = route in listOf(Screen.EditProfile.route, Screen.Conflict.route)

    BrandImageBackground(R.drawable.bg_white, Modifier.fillMaxWidth()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.rupai_logo_color), "RupAI", Modifier.size(28.dp))
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    if (nested) IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasWhite)
            )
        },
        bottomBar = { FloatingBottomNavigation(choices, route, onNavigate) },
        containerColor = Color.Transparent,
        content = content
    )
    }
}

@Composable
private fun FloatingBottomNavigation(
    choices: List<NavigationChoice>,
    route: String?,
    onNavigate: (String) -> Unit
) {
    val navHeight = if (LocalDensity.current.fontScale >= 1.5f) 80.dp else 64.dp
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            color = CanvasWhite,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, OutlineNeutral.copy(alpha = .12f))
        ) {
            BoxWithConstraints(
                Modifier.fillMaxWidth().height(navHeight).padding(4.dp).selectableGroup()
            ) {
                val selectedIndex = choices.indexOfFirst { it.route == route }.coerceAtLeast(0)
                val itemWidth = maxWidth / choices.size
                val indicatorOffset by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    label = "selected tab"
                )
                Box(
                    Modifier.offset(x = indicatorOffset).width(itemWidth).fillMaxHeight()
                        .padding(2.dp).background(ActionGreen, RoundedCornerShape(28.dp))
                )
                Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                    choices.forEach { choice ->
                        BottomNavigationItem(
                            choice = choice,
                            selected = choice.route == route,
                            onClick = { onNavigate(choice.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationItem(
    choice: NavigationChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor by animateColorAsState(
        if (selected) CanvasWhite else MutedInk,
        animationSpec = tween(220),
        label = "tab content"
    )
    Box(
        modifier.fillMaxHeight().selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(iconFor(choice.route, selected), contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Text(
                choice.label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun iconFor(route: String, selected: Boolean): ImageVector = when (route) {
    Screen.Cockpit.route -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    Screen.Plan.route -> if (selected) Icons.Filled.CalendarDots else Icons.Outlined.EventNote
    Screen.Copilot.route -> if (selected) Icons.Filled.Sparkle else Icons.Outlined.AutoAwesome
    Screen.Transactions.route -> if (selected) Icons.Filled.Receipt else Icons.Outlined.ReceiptLong
    Screen.Profile.route -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
    else -> Icons.Outlined.AccountCircle
}

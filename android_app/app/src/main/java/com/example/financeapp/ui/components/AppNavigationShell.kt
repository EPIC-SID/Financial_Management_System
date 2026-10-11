package com.example.financeapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.financeapp.data.ingest.Scenario
import com.example.financeapp.ui.navigation.*
import com.example.financeapp.ui.theme.*
import com.example.financeapp.ui.viewmodel.AppDataMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationShell(visible: Boolean, mode: AppDataMode, route: String?, scenario: Scenario,
    freelancer: Boolean, loading: Boolean, onNavigate: (String) -> Unit,
    onMode: (AppDataMode) -> Unit, onScenario: (Scenario, Boolean) -> Unit,
    onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    if (!visible) { content(PaddingValues()); return }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val choices = navigationChoices(mode)
    val title = choices.firstOrNull { it.route == route }?.label ?: when (route) {
        Screen.EditProfile.route -> "Edit profile"
        Screen.Conflict.route -> "Adjust your goal"
        else -> "RupAI"
    }
    val subtitle = if (mode == AppDataMode.LIVE) "Your SMS data" else
        "Sample data - ${if (freelancer) "Freelancer" else scenarioTitle(scenario)}"
    fun closeThen(action: () -> Unit) { scope.launch { drawer.close(); action() } }
    BackHandler(drawer.isOpen) { scope.launch { drawer.close() } }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet(Modifier.widthIn(max = 320.dp), drawerContainerColor = CanvasWhite) {
            Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("RupAI", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    IconButton(onClick = { scope.launch { drawer.close() } }) { Icon(Icons.Outlined.Close, "Close menu") }
                }
                choices.forEach { choice ->
                    NavigationDrawerItem(label = { Text(choice.label) }, selected = choice.route == route,
                        onClick = { closeThen { onNavigate(choice.route) } }, shape = RoundedCornerShape(8.dp),
                        icon = { Icon(when (choice.route) {
                            Screen.Cockpit.route -> Icons.Outlined.Home
                            Screen.Plan.route -> Icons.Outlined.EventNote
                            Screen.Copilot.route -> Icons.Outlined.AutoAwesome
                            Screen.Transactions.route -> Icons.Outlined.ReceiptLong
                            else -> Icons.Outlined.Person
                        }, null) }, colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = SummaryGreen,
                            selectedTextColor = Ink, selectedIconColor = Ink))
                }
                Divider(Modifier.padding(vertical = Spacing.sm))
                Text("Data source", style = MaterialTheme.typography.titleSmall)
                AppDataMode.entries.forEach { source ->
                    Row(Modifier.fillMaxWidth().selectable(selected = mode == source, role = Role.RadioButton) { closeThen { onMode(source) } }
                        .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(mode == source, onClick = null)
                        Text(if (source == AppDataMode.DEMO) "Sample data" else "My SMS transactions", Modifier.weight(1f))
                    }
                }
                if (mode == AppDataMode.DEMO) {
                    Divider(Modifier.padding(vertical = Spacing.sm))
                    Text("Sample situation", style = MaterialTheme.typography.titleSmall)
                    Scenario.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth().selectable(selected = !freelancer && scenario == option, enabled = !loading, role = Role.RadioButton) {
                            closeThen { onScenario(option, false) }
                        }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(!freelancer && scenario == option, onClick = null, enabled = !loading)
                            Text(scenarioTitle(option), Modifier.weight(1f))
                        }
                    }
                    Row(Modifier.fillMaxWidth().selectable(selected = freelancer, enabled = !loading, role = Role.RadioButton) {
                        closeThen { onScenario(Scenario.STABLE_INCOME, true) }
                    }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(freelancer, onClick = null, enabled = !loading)
                        Text("Freelancer", Modifier.weight(1f))
                    }
                }
            }
        }
    }) {
        Scaffold(topBar = {
            TopAppBar(title = { Column(Modifier.clickable { scope.launch { drawer.open() } }) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MutedInk)
            } }, navigationIcon = {
                if (route in listOf(Screen.EditProfile.route, Screen.Conflict.route))
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                else IconButton(onClick = { scope.launch { drawer.open() } }) { Icon(Icons.Outlined.Menu, "Open menu") }
            }, actions = {
                if (route != Screen.Profile.route && route != Screen.EditProfile.route)
                    IconButton(onClick = { onNavigate(Screen.Profile.route) }) { Icon(Icons.Outlined.AccountCircle, "Open profile") }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasWhite))
        }, containerColor = CanvasWhite, content = content)
    }
}

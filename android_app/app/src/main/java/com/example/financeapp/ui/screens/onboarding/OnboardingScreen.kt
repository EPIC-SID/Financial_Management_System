package com.example.financeapp.ui.screens.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.example.financeapp.ui.icons.Icons
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.financeapp.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.ui.components.RupAiButton
import com.example.financeapp.ui.components.RupAiButtonSize
import com.example.financeapp.ui.components.RupAiButtonState
import com.example.financeapp.ui.components.BrandImageBackground
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.ErrorInk
import com.example.financeapp.ui.theme.ErrorPink
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.GuidanceYellow
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SummaryGreen
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.ui.viewmodel.IngestRowUi
import com.example.financeapp.ui.viewmodel.IngestStatus
import com.example.financeapp.ui.viewmodel.OnboardingDataSource
import com.example.financeapp.ui.viewmodel.OnboardingFixtures
import com.example.financeapp.ui.viewmodel.OnboardingPage
import com.example.financeapp.ui.viewmodel.OnboardingUiState
import com.example.financeapp.ui.viewmodel.OnboardingViewModel
import com.example.financeapp.ui.viewmodel.SmsPermissionState
import com.example.financeapp.ui.viewmodel.formatOnboardingCurrency

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onPrimary: () -> Unit = {},
    onBack: () -> Unit = {},
    onExistingAccount: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
    showAccountActions: Boolean = true,
    onSourceSelected: (OnboardingDataSource) -> Unit = {},
    onAlternativeSelected: (OnboardingDataSource) -> Unit = {},
    onMoneyArrivalSelected: (String) -> Unit = {},
    onObligationToggled: (String) -> Unit = {},
    onCushionChanged: (String) -> Unit = {},
    showSmsOption: Boolean = true,
    privacyContent: @Composable () -> Unit = {}
) {
    val primaryEnabled = when (state.page) {
        OnboardingPage.ABOUT_MONEY -> if (state.moneyStep == 0) state.moneyArrival != null else state.obligations.isNotEmpty()
        OnboardingPage.SAFETY_NET -> state.cushionSelected
        OnboardingPage.BUILDING -> state.completeness != null
        else -> true
    }
    BrandImageBackground(R.drawable.bg_white, Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            OnboardingActions(
                state = state,
                primaryEnabled = primaryEnabled,
                onPrimary = onPrimary,
                onCreateAccount = onCreateAccount,
                showAccountActions = showAccountActions
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(top = Spacing.lg, bottom = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            item {
                OnboardingHeader(
                    page = state.page,
                    showSignIn = state.page == OnboardingPage.WELCOME,
                    onSignIn = onExistingAccount,
                    onBack = onBack
                )
            }
            item { SegmentedProgress(state.page, Modifier.padding(horizontal = Spacing.md)) }
            state.error?.let { error -> item {
                Text(error, Modifier.padding(horizontal = Spacing.md), color = ErrorInk)
            } }
            item { OnboardingIllustration(state) }
            item {
                Text(
                    headlineFor(state),
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            }
            item {
                Box(Modifier.padding(horizontal = Spacing.md)) {
                    when (state.page) {
                        OnboardingPage.WELCOME -> WelcomeContent()
                        OnboardingPage.DATA_SOURCE -> DataSourceContent(state, onSourceSelected, showSmsOption)
                        OnboardingPage.SMS_CONSENT -> SmsConsentContent(state.permissionState, onAlternativeSelected)
                        OnboardingPage.ABOUT_MONEY -> AboutMoneyContent(
                            state, onMoneyArrivalSelected, onObligationToggled
                        )
                        OnboardingPage.SAFETY_NET -> Column {
                            SafetyNetContent(state, onCushionChanged)
                            privacyContent()
                        }
                        OnboardingPage.BUILDING -> BuildingContent(state, onAlternativeSelected)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun OnboardingActions(
    state: OnboardingUiState,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onCreateAccount: () -> Unit,
    showAccountActions: Boolean
) {
    if (state.page == OnboardingPage.DATA_SOURCE) return
    val label = when (state.page) {
        OnboardingPage.WELCOME -> if (showAccountActions) "Explore RupAI" else "Start setup"
        OnboardingPage.DATA_SOURCE -> error("Handled above")
        OnboardingPage.SMS_CONSENT -> when (state.permissionState) {
            SmsPermissionState.EXPLAINER -> "Allow SMS access"
            SmsPermissionState.DENIED -> "Try again"
            SmsPermissionState.PERMANENTLY_DENIED -> "Open Android settings"
        }
        OnboardingPage.ABOUT_MONEY -> if (state.moneyStep == 0) "Continue" else "Confirm answers"
        OnboardingPage.SAFETY_NET -> "Confirm safety net"
        OnboardingPage.BUILDING -> "Open RupAI"
    }
    Surface(shadowElevation = 3.dp, color = CanvasWhite) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RupAiButton(
                label = label,
                onClick = onPrimary,
                size = RupAiButtonSize.DEFAULT,
                state = if (state.busy) RupAiButtonState.LOADING else if (primaryEnabled) RupAiButtonState.DEFAULT else RupAiButtonState.DISABLED,
                disabledReason = when (state.page) {
                    OnboardingPage.BUILDING -> "We’re still building your picture."
                    else -> "Complete this step to continue."
                }
            )
            when (state.page) {
                OnboardingPage.BUILDING -> if (showAccountActions && state.completeness != null) {
                    TextButton(onClick = onCreateAccount, enabled = !state.busy) { Text("Create account", color = Ink) }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun OnboardingHeader(page: OnboardingPage, showSignIn: Boolean, onSignIn: () -> Unit, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            if (page != OnboardingPage.WELCOME && page != OnboardingPage.BUILDING) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
            }
            Image(
                painter = painterResource(R.drawable.rupai_logo_mono),
                contentDescription = "RupAI",
                colorFilter = ColorFilter.tint(Ink),
                modifier = Modifier.size(36.dp)
            )
        }
        if (showSignIn) {
            Surface(
                onClick = onSignIn,
                modifier = Modifier.heightIn(min = Spacing.minimumTouchTarget),
                shape = RoundedCornerShape(Radius.button),
                border = BorderStroke(1.dp, Ink),
                color = CanvasWhite
            ) {
                Box(Modifier.padding(horizontal = Spacing.md), contentAlignment = Alignment.Center) {
                    Text("Sign in", style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SegmentedProgress(page: OnboardingPage, modifier: Modifier = Modifier) {
    val current = page.ordinal
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = "Step ${current + 1} of 6" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        repeat(6) { index ->
            Box(
                Modifier.weight(1f).height(4.dp).clip(CircleShape).background(
                    when {
                        index < current -> ActionGreen
                        index == current -> GuidanceYellow
                        else -> SurfaceNeutral
                    }
                )
            )
        }
    }
}

@Composable
private fun WelcomeContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("See your spending, bills and goals in one simple view.", style = MaterialTheme.typography.bodyMedium)
        listOf("No bank passwords", "Bank alerts are read on this phone", "You choose what gets saved").forEach {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp), tint = ActionGreen)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DataSourceContent(
    state: OnboardingUiState,
    onSelected: (OnboardingDataSource) -> Unit,
    showSmsOption: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OnboardingDataSource.entries.filter { it != OnboardingDataSource.CSV && (showSmsOption || it != OnboardingDataSource.SMS) }.forEach { source ->
            SelectableCard(source, selected = state.selectedSource == source, onClick = { onSelected(source) })
        }
        if (state.selectedSource == OnboardingDataSource.SAMPLE) {
            Text("Sample profile: regular income, EMIs and monthly bills", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
        }
    }
}

@Composable
private fun SelectableCard(source: OnboardingDataSource, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().heightIn(min = 96.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        shape = RoundedCornerShape(Radius.card),
        color = if (selected) SummaryGreen else CanvasWhite,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) ActionGreen else Ink)
    ) {
        Row(
            Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    if (source == OnboardingDataSource.SMS) {
                        Surface(shape = CircleShape, color = GuidanceYellow) {
                            Text("Fastest", Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text(source.detail, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
            if (selected) Icon(Icons.Outlined.CheckCircle, "Selected", tint = ActionGreen)
        }
    }
}

@Composable
private fun SmsConsentContent(
    permissionState: SmsPermissionState,
    onAlternativeSelected: (OnboardingDataSource) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (permissionState != SmsPermissionState.EXPLAINER) {
            Notice(
                title = if (permissionState == SmsPermissionState.DENIED) "SMS access is still off" else "Turn on SMS access in Android settings",
                detail = if (permissionState == SmsPermissionState.DENIED) "Try again, or start with a sample profile."
                else "You can turn it on in Settings, or start with a sample profile."
            )
        }
        ReadList("Used to find transactions", listOf("Supported bank alerts", "Amount, date and merchant"), SummaryGreen)
        ReadList("Left out", listOf("Personal chats", "OTPs", "Contacts"), SurfaceNeutral)
        if (permissionState != SmsPermissionState.EXPLAINER) {
            PermissionRecoveryActions(onAlternativeSelected)
        }
        Text("The first scan starts only when you tap Sync. Raw messages stay on this phone and are never sent to the AI provider.", style = MaterialTheme.typography.bodySmall, color = MutedInk)
    }
}

@Composable
private fun ReadList(title: String, items: List<String>, color: Color) {
    Surface(shape = RoundedCornerShape(Radius.card), color = color) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            items.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun Notice(title: String, detail: String) {
    Surface(shape = RoundedCornerShape(Radius.card), color = ErrorPink, contentColor = ErrorInk) {
        Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AboutMoneyContent(
    state: OnboardingUiState,
    onArrival: (String) -> Unit,
    onObligation: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Step ${state.moneyStep + 1} of 2", style = MaterialTheme.typography.labelLarge, color = MutedInk)
        when (state.moneyStep) {
            0 -> ChoiceChips("Choose one", listOf("Fixed salary", "Varies month to month", "A mix"), setOfNotNull(state.moneyArrival), onArrival)
            else -> {
                ChoiceChips(
                    "Choose all that apply",
                    listOf("Rent", "Home loan", "Vehicle EMI", "Credit card", "Gadget EMI", "Insurance", "Subscriptions", "Mutual fund SIP", "None"),
                    state.obligations,
                    onObligation
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ChoiceChips(title: String, choices: List<String>, selected: Set<String>, onClick: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        choices.forEach { choice ->
            val isSelected = choice in selected
            Surface(
                modifier = Modifier.heightIn(min = Spacing.minimumTouchTarget)
                    .toggleable(value = isSelected, role = Role.Checkbox) { onClick(choice) },
                shape = CircleShape,
                color = if (isSelected) SummaryGreen else CanvasWhite,
                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) ActionGreen else Ink)
            ) {
                Row(
                    Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                    Text(choice, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SafetyNetContent(
    state: OnboardingUiState,
    onCushionChanged: (String) -> Unit
) {
    val selected = when (state.cushionDigits) {
        "30000" -> setOf("₹30,000")
        "60000" -> setOf("₹60,000")
        else -> if (state.cushionSelected) setOf("Decide later") else emptySet()
    }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Choose one. You can change it later from Profile.", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
        ChoiceChips(
            title = "One tap and you’re done",
            choices = listOf("₹30,000", "₹60,000", "Decide later"),
            selected = selected,
            onClick = { choice -> onCushionChanged(cushionValueFor(choice)) }
        )
    }
}

internal fun cushionValueFor(choice: String): String = when (choice) {
    "₹30,000" -> "30000"
    "₹60,000" -> "60000"
    else -> ""
}

@Composable
private fun BuildingContent(
    state: OnboardingUiState,
    onAlternativeSelected: (OnboardingDataSource) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        state.ingestRows.forEach { IngestRow(it) }
        state.completeness?.let {
            Surface(shape = RoundedCornerShape(Radius.card), color = SummaryGreen) {
                Column(Modifier.fillMaxWidth().padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text("Setup complete", style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (state.ingestFailed) {
            Notice("We couldn’t build a picture from those alerts", "Nothing was changed. Start with a sample profile instead.")
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                AssistChip(onClick = { onAlternativeSelected(OnboardingDataSource.CSV) }, label = { Text("Upload CSV") })
                AssistChip(onClick = { onAlternativeSelected(OnboardingDataSource.SAMPLE) }, label = { Text("Use sample") })
            }
        }
    }
}

@Composable
private fun IngestRow(row: IngestRowUi) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Spacing.minimumTouchTarget).semantics {
            contentDescription = "${row.label}, ${row.status.name.lowercase()}${if (row.detail.isBlank()) "" else ", ${row.detail}"}"
        },
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Crossfade(row.status, label = "ingest status") { status ->
            Icon(
                imageVector = when (status) {
                    IngestStatus.DONE -> Icons.Outlined.CheckCircle
                    IngestStatus.RUNNING -> Icons.Outlined.HourglassTop
                    IngestStatus.FAILED -> Icons.Outlined.ErrorOutline
                },
                contentDescription = null,
                tint = if (status == IngestStatus.FAILED) ErrorInk else ActionGreen
            )
        }
        Column {
            Text(row.label, style = MaterialTheme.typography.bodyLarge)
            if (row.detail.isNotBlank()) Text(row.detail, style = MaterialTheme.typography.bodySmall, color = MutedInk)
        }
    }
}

private fun headlineFor(state: OnboardingUiState): String = when (state.page) {
    OnboardingPage.WELCOME -> "See your money clearly."
    OnboardingPage.DATA_SOURCE -> "How would you like to begin?"
    OnboardingPage.SMS_CONSENT -> "Use bank alerts without uploading messages."
    OnboardingPage.ABOUT_MONEY -> if (state.moneyStep == 0) "How predictable is your income?" else "Which bills come back every month?"
    OnboardingPage.SAFETY_NET -> "How much would you like to keep untouched?"
    OnboardingPage.BUILDING -> if (state.ingestFailed) "No problem. Try another way." else "Your starting picture is ready."
}

private class OnboardingPreviewProvider : CollectionPreviewParameterProvider<OnboardingUiState>(OnboardingFixtures.previews)

@Preview(name = "Onboarding states · 360 · 100%", widthDp = 360, heightDp = 800, fontScale = 1f, showBackground = true)
@Preview(name = "Onboarding states · 412 · 200%", widthDp = 412, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
private fun OnboardingPreview(@PreviewParameter(OnboardingPreviewProvider::class) state: OnboardingUiState) {
    FinanceAppTheme { OnboardingScreen(state) }
}

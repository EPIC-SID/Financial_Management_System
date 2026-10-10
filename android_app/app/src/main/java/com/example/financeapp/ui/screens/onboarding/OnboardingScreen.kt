package com.example.financeapp.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.R
import com.example.financeapp.ui.components.RupAiButton
import com.example.financeapp.ui.components.RupAiButtonSize
import com.example.financeapp.ui.components.RupAiButtonState
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
fun OnboardingRoute(
    viewModel: OnboardingViewModel = viewModel(),
    onExistingAccount: () -> Unit = {},
    onComplete: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    OnboardingScreen(
        state = state,
        onPrimary = {
            if (state.page == OnboardingPage.BUILDING && state.completeness != null) onComplete()
            else if (state.page == OnboardingPage.SMS_CONSENT && state.permissionState == SmsPermissionState.PERMANENTLY_DENIED) onOpenSettings()
            else viewModel.next()
        },
        onBack = viewModel::back,
        onExistingAccount = onExistingAccount,
        onSourceSelected = viewModel::selectSource,
        onAlternativeSelected = viewModel::chooseAlternative,
        onMoneyArrivalSelected = viewModel::setMoneyArrival,
        onObligationToggled = viewModel::toggleObligation,
        onAgeRangeSelected = viewModel::setAgeRange,
        onCushionChanged = viewModel::setCushion,
        onGoalNameChanged = viewModel::setGoalName,
        onGoalAmountChanged = viewModel::setGoalAmount
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onPrimary: () -> Unit = {},
    onBack: () -> Unit = {},
    onExistingAccount: () -> Unit = {},
    onSourceSelected: (OnboardingDataSource) -> Unit = {},
    onAlternativeSelected: (OnboardingDataSource) -> Unit = {},
    onMoneyArrivalSelected: (String) -> Unit = {},
    onObligationToggled: (String) -> Unit = {},
    onAgeRangeSelected: (String) -> Unit = {},
    onCushionChanged: (String) -> Unit = {},
    onGoalNameChanged: (String) -> Unit = {},
    onGoalAmountChanged: (String) -> Unit = {}
) {
    val primaryEnabled = when (state.page) {
        OnboardingPage.DATA_SOURCE -> state.selectedSource != null
        OnboardingPage.BUILDING -> state.completeness != null
        else -> true
    }
    Scaffold(
        containerColor = CanvasWhite,
        bottomBar = {
            OnboardingActions(
                state = state,
                primaryEnabled = primaryEnabled,
                onPrimary = onPrimary,
                onBack = onBack,
                onExistingAccount = onExistingAccount
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item { SegmentedProgress(state.page) }
            item { OnboardingIllustration(illustrationFor(state), illustrationTint(state)) }
            item {
                Text(
                    headlineFor(state),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Ink
                )
            }
            item {
                when (state.page) {
                    OnboardingPage.WELCOME -> WelcomeContent()
                    OnboardingPage.DATA_SOURCE -> DataSourceContent(state, onSourceSelected)
                    OnboardingPage.SMS_CONSENT -> SmsConsentContent(state.permissionState)
                    OnboardingPage.ABOUT_MONEY -> AboutMoneyContent(
                        state, onMoneyArrivalSelected, onObligationToggled, onAgeRangeSelected
                    )
                    OnboardingPage.SAFETY_NET -> SafetyNetContent(
                        state, onCushionChanged, onGoalNameChanged, onGoalAmountChanged
                    )
                    OnboardingPage.BUILDING -> BuildingContent(state, onAlternativeSelected)
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
    onBack: () -> Unit,
    onExistingAccount: () -> Unit
) {
    val label = when (state.page) {
        OnboardingPage.WELCOME -> "Get started"
        OnboardingPage.DATA_SOURCE -> "Continue"
        OnboardingPage.SMS_CONSENT -> when (state.permissionState) {
            SmsPermissionState.EXPLAINER -> "Allow SMS access"
            SmsPermissionState.DENIED -> "Try again"
            SmsPermissionState.PERMANENTLY_DENIED -> "Open Android settings"
        }
        OnboardingPage.ABOUT_MONEY -> "Next"
        OnboardingPage.SAFETY_NET -> "Build my picture"
        OnboardingPage.BUILDING -> "See my picture"
    }
    Surface(shadowElevation = 3.dp, color = CanvasWhite) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RupAiButton(
                label = label,
                onClick = onPrimary,
                size = RupAiButtonSize.LARGE,
                state = if (primaryEnabled) RupAiButtonState.DEFAULT else RupAiButtonState.DISABLED,
                disabledReason = when (state.page) {
                    OnboardingPage.DATA_SOURCE -> "Choose one option to continue."
                    OnboardingPage.BUILDING -> "We’re still building your picture."
                    else -> "Complete this step to continue."
                }
            )
            when (state.page) {
                OnboardingPage.WELCOME -> TextButton(onClick = onExistingAccount) { Text("I already have an account") }
                OnboardingPage.SMS_CONSENT -> TextButton(onClick = onBack) { Text("Not now") }
                OnboardingPage.ABOUT_MONEY, OnboardingPage.SAFETY_NET -> TextButton(onClick = onBack) { Text("Back") }
                else -> Unit
            }
        }
    }
}

@Composable
private fun SegmentedProgress(page: OnboardingPage) {
    val current = page.ordinal
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = "Step ${current + 1} of 6" },
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
private fun OnboardingIllustration(@DrawableRes resource: Int, tint: Color) {
    Surface(
        Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(Radius.card),
        color = tint
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(Spacing.sm)
        )
    }
}

@Composable
private fun WelcomeContent() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Bring your balances, dues and plans into one clear picture.", style = MaterialTheme.typography.bodyLarge)
        listOf("Stays on your phone", "Never asks for bank passwords", "Delete anytime").forEach {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Icon(Icons.Outlined.CheckCircle, null, tint = ActionGreen)
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DataSourceContent(state: OnboardingUiState, onSelected: (OnboardingDataSource) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OnboardingDataSource.entries.forEach { source ->
            SelectableCard(source, selected = state.selectedSource == source, onClick = { onSelected(source) })
        }
        if (state.selectedSource == OnboardingDataSource.SAMPLE) {
            Text("Sample: Mid-career with EMIs", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
        }
    }
}

@Composable
private fun SelectableCard(source: OnboardingDataSource, selected: Boolean, onClick: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().heightIn(min = 96.dp).clickable(role = Role.RadioButton, onClick = onClick),
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
                            Text("Recommended", Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xxs), style = MaterialTheme.typography.labelSmall)
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
private fun SmsConsentContent(permissionState: SmsPermissionState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (permissionState != SmsPermissionState.EXPLAINER) {
            Notice(
                title = if (permissionState == SmsPermissionState.DENIED) "SMS access wasn’t allowed" else "Allow access in Android settings",
                detail = if (permissionState == SmsPermissionState.DENIED) "You can try again, upload a CSV or use a sample profile."
                else "Permission was turned off permanently. Open settings to change it, or choose another way to add data."
            )
        }
        ReadList("What we read", listOf("Supported bank alerts", "Amount, date and merchant"), SummaryGreen)
        ReadList("What we never read", listOf("Personal chats", "OTPs", "Contacts"), SurfaceNeutral)
        Text("RupAI filters bank alerts in the app. AI categorization may send merchant details and message snippets to our backend.", style = MaterialTheme.typography.bodySmall, color = MutedInk)
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
    onObligation: (String) -> Unit,
    onAgeRange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Question ${state.moneyStep + 1} of 3", style = MaterialTheme.typography.labelLarge, color = MutedInk)
        when (state.moneyStep) {
            0 -> ChoiceChips("How does your money arrive?", listOf("Fixed salary", "Varies month to month", "A mix"), setOfNotNull(state.moneyArrival), onArrival)
            1 -> ChoiceChips(
                "What payments are fixed?",
                listOf("Rent", "Home loan", "Vehicle EMI", "Credit card", "Gadget EMI", "Insurance", "Subscriptions", "Mutual fund SIP", "None"),
                state.obligations,
                onObligation
            )
            else -> {
                ChoiceChips("Age range (optional)", listOf("18–25", "26–38", "39–50", "Prefer not to say"), setOfNotNull(state.ageRange), onAgeRange)
                Text("Only used to tune wording.", style = MaterialTheme.typography.bodySmall, color = MutedInk)
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
                    .clickable(role = Role.Checkbox) { onClick(choice) },
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
    onCushionChanged: (String) -> Unit,
    onGoalNameChanged: (String) -> Unit,
    onGoalAmountChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text("Skip anything you don’t know. We’ll show how sure we are.", style = MaterialTheme.typography.bodyLarge)
        OutlinedTextField(
            value = formatOnboardingCurrency(state.cushionDigits),
            onValueChange = onCushionChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Minimum cash cushion") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AssistChip(onClick = { onCushionChanged("30000") }, label = { Text("1 month essentials") })
            AssistChip(onClick = { onCushionChanged("60000") }, label = { Text("2 months") })
            AssistChip(onClick = { onCushionChanged("") }, label = { Text("Custom") })
        }
        Text("Optional goal", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.goalName,
            onValueChange = onGoalNameChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Goal name") },
            singleLine = true
        )
        OutlinedTextField(
            value = formatOnboardingCurrency(state.goalDigits),
            onValueChange = onGoalAmountChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Goal amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Text("Loan, card and SIP details can be added later if you know them.", style = MaterialTheme.typography.bodySmall, color = MutedInk)
    }
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
                    Text("Your picture is ready", style = MaterialTheme.typography.titleMedium)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (state.ingestFailed) {
            Notice("We couldn’t find enough to build your picture", "Your choices are safe. Upload a CSV or start with a sample profile.")
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
    OnboardingPage.WELCOME -> "Know where you stand before the month ends."
    OnboardingPage.DATA_SOURCE -> "Start with the picture you have today."
    OnboardingPage.SMS_CONSENT -> "Your messages stay under your control."
    OnboardingPage.ABOUT_MONEY -> "Tell us what shapes your month."
    OnboardingPage.SAFETY_NET -> "What keeps you comfortable?"
    OnboardingPage.BUILDING -> if (state.ingestFailed) "Let’s try another way." else "Building your picture"
}

@DrawableRes
private fun illustrationFor(state: OnboardingUiState): Int = when (state.page) {
    OnboardingPage.WELCOME -> R.drawable.welcome
    OnboardingPage.DATA_SOURCE -> R.drawable.onboarding_choices
    OnboardingPage.SMS_CONSENT -> R.drawable.privacy_control
    OnboardingPage.ABOUT_MONEY -> R.drawable.onboarding_first_step
    OnboardingPage.SAFETY_NET -> R.drawable.plan_commitments
    OnboardingPage.BUILDING -> if (state.ingestFailed) R.drawable.error_recovery else R.drawable.ledger_organizer
}

private fun illustrationTint(state: OnboardingUiState): Color = when {
    state.ingestFailed -> ErrorPink
    state.page == OnboardingPage.SAFETY_NET -> GuidanceYellow.copy(alpha = .22f)
    else -> SummaryGreen
}

private class OnboardingPreviewProvider : CollectionPreviewParameterProvider<OnboardingUiState>(OnboardingFixtures.previews)

@Preview(name = "Onboarding states · 360 · 100%", widthDp = 360, heightDp = 800, fontScale = 1f, showBackground = true)
@Preview(name = "Onboarding states · 412 · 200%", widthDp = 412, heightDp = 900, fontScale = 2f, showBackground = true)
@Composable
private fun OnboardingPreview(@PreviewParameter(OnboardingPreviewProvider::class) state: OnboardingUiState) {
    FinanceAppTheme { OnboardingScreen(state) }
}

package com.example.financeapp.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.domain.engine.CashFlowPeriod
import com.example.financeapp.domain.engine.ImpactResult
import com.example.financeapp.domain.engine.ImpactSimulator
import com.example.financeapp.domain.engine.SimulationInputs
import com.example.financeapp.domain.engine.SimulationLoan
import com.example.financeapp.domain.engine.WhatIfChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Baseline risk and interest saved must describe the same horizon and policy as [inputs]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatIfSheet(
    inputs: SimulationInputs,
    baselineShortfallRisk: Double,
    onDismissRequest: () -> Unit,
    onApplyToPlan: (WhatIfChange) -> Unit,
    initialChange: WhatIfChange = WhatIfChange(),
    baselineInterestSaved: Double = 0.0,
    applyLabel: String = "Apply to Plan"
) {
    require(baselineShortfallRisk.isFinite() && baselineShortfallRisk in 0.0..1.0)
    require(baselineInterestSaved.isFinite())
    val discretionary = inputs.periods.map { it.discretionary }.average()
    val currentSip = inputs.periods.maxOf { it.sip }
    val debt = inputs.loans.sumOf { it.outstanding }
    var cut by rememberSaveable(inputs, initialChange) { mutableDoubleStateOf(initialChange.discretionaryCut * discretionary) }
    var prepayment by rememberSaveable(inputs, initialChange) { mutableDoubleStateOf(initialChange.extraPrepayment) }
    var income by rememberSaveable(inputs, initialChange) { mutableDoubleStateOf(initialChange.incomeChange * 100) }
    var sipReduction by rememberSaveable(inputs, initialChange) { mutableDoubleStateOf(initialChange.sipReduction) }
    var paused by rememberSaveable(inputs, initialChange) { mutableStateOf(currentSip > 0 && initialChange.sipReduction >= currentSip) }
    var expense by rememberSaveable(inputs, initialChange) { mutableStateOf(initialChange.oneOffExpense.roundToLong().toString()) }
    var expensePeriod by rememberSaveable(inputs, initialChange) { mutableIntStateOf(initialChange.expensePeriod) }
    val expenseAmount = expense.toDoubleOrNull() ?: if (expense.isEmpty()) 0.0 else Double.NaN
    val expenseError = !expenseAmount.isFinite() || expenseAmount !in 0.0..50000.0
    val cutError = cut !in 0.0..10000.0 || cut > discretionary * 0.6
    val debtError = prepayment !in 0.0..15000.0 || prepayment > debt
    val valid = !expenseError && !cutError && !debtError && income in -50.0..50.0 &&
        sipReduction in 0.0..currentSip && expensePeriod in 1..inputs.periods.size
    val change = if (valid) WhatIfChange(
        discretionaryCut = if (discretionary == 0.0) 0.0 else cut / discretionary,
        incomeChange = income / 100,
        oneOffExpense = expenseAmount,
        expensePeriod = expensePeriod,
        sipReduction = if (paused) currentSip else sipReduction,
        extraPrepayment = prepayment
    ) else null
    val computed by produceState<SheetSimulation?>(null, inputs, change) {
        value = null
        if (change != null) {
            try {
                value = withContext(Dispatchers.Default) {
                    simulateSheet(inputs, change)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                value = SheetSimulation(inputs, change, error = true)
            }
        }
    }
    val result = if (LocalInspectionMode.current) {
        remember(inputs, change) { change?.let { simulateSheet(inputs, it) } }
    } else computed?.takeIf { it.inputs == inputs && it.change == change }
    val dark = isSystemInDarkTheme()
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFE3A25E), onPrimary = Color(0xFF1F1A14),
        surface = Color(0xFF1D1914), onSurface = Color(0xFFF2EBE1),
        surfaceVariant = Color(0xFF2A241D), onSurfaceVariant = Color(0xFFB5AA9A), outline = Color(0xFF3D352B)
    ) else lightColorScheme(
        primary = Color(0xFFCD7F32), onPrimary = Color(0xFF1F1A14),
        surface = Color(0xFFFBF8F4), onSurface = Color(0xFF1F1A14),
        surfaceVariant = Color(0xFFF3ECE3), onSurfaceVariant = Color(0xFF6B6257), outline = Color(0xFFD9CFC2)
    )
    MaterialTheme(colorScheme = colors) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = colors.surface,
            scrimColor = Color.Black.copy(alpha = 0.32f)
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.8f).dp)
                .imePadding().padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("What-if simulation", Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.titleLarge, letterSpacing = 0.sp)
                    IconButton(onClick = onDismissRequest) { Icon(Icons.Default.Close, "Close simulation") }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Predicted \u00b7 ${inputs.periods.size} months", color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium, letterSpacing = 0.sp)
                    if (result?.impact != null) {
                        val impact = result.impact
                        MetricComparison("Liquid runway", months(result.beforeRunway), months(result.afterRunway),
                            "${signed(impact.runwayDeltaMonths)} mo", impact.runwayDeltaMonths, higherIsBetter = true)
                        MetricComparison("Shortfall risk", percent(baselineShortfallRisk),
                            percent((baselineShortfallRisk + impact.shortfallRiskDelta).coerceIn(0.0, 1.0)),
                            "${signed(impact.shortfallRiskDelta * 100)} pp", impact.shortfallRiskDelta, higherIsBetter = false)
                        MetricComparison("Projected interest saved", inr(baselineInterestSaved),
                            inr(baselineInterestSaved + impact.interestSaved), signedInr(impact.interestSaved),
                            impact.interestSaved, higherIsBetter = true)
                    } else {
                        Text(when {
                            !valid -> "Review the highlighted amounts to calculate impact."
                            result?.error == true -> "Impact could not be calculated. Adjust an amount or reset."
                            else -> "Calculating impact..."
                        }, Modifier.fillMaxWidth().padding(vertical = 16.dp), color = colors.onSurfaceVariant)
                    }
                    Divider(color = colors.outline)
                    val maximumCut = minOf(10000.0, discretionary * 0.6)
                    AmountSlider("Bank-paid discretionary cut", cut, maximumCut,
                        if (maximumCut < 500.0) 0.0 else 500.0, ::inr,
                        enabled = discretionary > 0.0, onChange = { cut = it })
                    if (cutError) FieldError("Maximum cut: ${inr(minOf(10000.0, discretionary * 0.6))} (60% of discretionary spend).")
                    AmountSlider("Extra debt prepayment", prepayment, 15000.0, 1000.0, ::inr,
                        enabled = debt > 0.0, onChange = { prepayment = it })
                    if (debtError) FieldError("Available debt: ${inr(debt)}. Reduce the prepayment.")
                    if (debt == 0.0) Text("No debt available for prepayment", color = colors.onSurfaceVariant)
                    AmountSlider("Income change", income, 50.0, 5.0, { "${signed(it)}%" },
                        minimum = -50.0, onChange = { income = it })
                    if (income !in -50.0..50.0) FieldError("Income change must be between -50% and +50%.")
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pause SIP", style = MaterialTheme.typography.titleSmall)
                            Text("Current SIP: ${inr(currentSip)}", color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = paused, onCheckedChange = { paused = it }, enabled = currentSip > 0,
                            modifier = Modifier.semantics { contentDescription = "Pause SIP" })
                    }
                    if (currentSip > 0) AmountSlider("SIP reduction", if (paused) currentSip else sipReduction,
                        currentSip, 0.0, ::inr, enabled = !paused, onChange = { sipReduction = it })
                    if (sipReduction !in 0.0..currentSip) FieldError("SIP reduction exceeds the current SIP. Adjust it or reset.")
                    OutlinedTextField(value = expense, onValueChange = { proposed ->
                        if (proposed.length <= 8 && proposed.all { it in '0'..'9' }) expense = proposed
                    }, label = { Text("One-time surprise expense") }, prefix = { Text("\u20b9") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        isError = expenseError, supportingText = {
                            Text(if (expenseError) "Enter an amount from \u20b90 to \u20b950,000" else "Maximum ${inr(50000.0)}")
                        }, modifier = Modifier.fillMaxWidth())
                    if (expensePeriod != 1) Text("Expense in month $expensePeriod", color = colors.onSurfaceVariant)
                    if (expensePeriod !in 1..inputs.periods.size) FieldError("Expense month is outside this forecast. Reset to use month 1.")
                    Spacer(Modifier.height(8.dp))
                }
                Divider(color = colors.outline)
                Button(onClick = { change?.let(onApplyToPlan) }, enabled = change != null && result?.impact != null,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(applyLabel, letterSpacing = 0.sp)
                }
                TextButton(onClick = {
                    cut = 0.0; prepayment = 0.0; income = 0.0; sipReduction = 0.0
                    paused = false; expense = "0"; expensePeriod = 1
                }, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = if (dark) Color(0xFFF0C494) else Color(0xFF8C531B))) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset", letterSpacing = 0.sp)
                }
            }
        }
    }
}

private data class SheetSimulation(
    val inputs: SimulationInputs,
    val change: WhatIfChange,
    val impact: ImpactResult? = null,
    val beforeRunway: Double = 0.0,
    val afterRunway: Double = 0.0,
    val error: Boolean = false
)

private fun simulateSheet(inputs: SimulationInputs, change: WhatIfChange): SheetSimulation {
    val simulator = ImpactSimulator()
    val before = simulator.simulate(inputs, WhatIfChange())
    val after = simulator.simulate(inputs, change)
    val cost = inputs.periods.map { it.essentials + it.debtService }.average()
    fun runway(result: ImpactResult): Double {
        val available = (result.periods.last().ending - inputs.minReserve).coerceAtLeast(0.0)
        return if (cost > 0.0) available / cost else if (available == 0.0) 0.0 else Double.POSITIVE_INFINITY
    }
    return SheetSimulation(inputs, change, after, runway(before), runway(after))
}

@Composable
private fun AmountSlider(label: String, value: Double, maximum: Double, step: Double,
                         format: (Double) -> String, minimum: Double = 0.0,
                         enabled: Boolean = true, onChange: (Double) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall, letterSpacing = 0.sp)
        Text(format(value), style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"), letterSpacing = 0.sp)
        Slider(value = value.coerceIn(minimum, maximum).toFloat(), onValueChange = { raw ->
            val snapped = if (step > 0) minimum + ((raw - minimum) / step).roundToInt() * step else raw.toDouble()
            onChange(snapped.coerceIn(minimum, maximum))
        }, valueRange = minimum.toFloat()..maximum.toFloat(),
            steps = if (step > 0) (((maximum - minimum) / step).roundToInt() - 1).coerceAtLeast(0) else 0,
            enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { contentDescription = label })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(format(minimum), style = MaterialTheme.typography.labelSmall)
            Text(format(maximum), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun MetricComparison(label: String, before: String, after: String, delta: String,
                             numericDelta: Double, higherIsBetter: Boolean) {
    val changed = abs(numericDelta) > 0.00001
    val improved = changed && (numericDelta > 0) == higherIsBetter
    val dark = isSystemInDarkTheme()
    val tint = when {
        !changed -> MaterialTheme.colorScheme.onSurfaceVariant
        improved -> if (dark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)
        else -> if (dark) Color(0xFFFFB4AB) else Color(0xFFB3261E)
    }
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, letterSpacing = 0.sp)
        Text("$before \u2192 $after", style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), letterSpacing = 0.sp)
        Surface(color = tint.copy(alpha = 0.10f), shape = RoundedCornerShape(6.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (changed) {
                    Icon(if (numericDelta > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        null, Modifier.size(16.dp), tint = tint)
                    Spacer(Modifier.width(4.dp))
                }
                Text("$delta \u00b7 ${if (!changed) "Unchanged" else if (improved) "Improved" else "Reduced safety"}",
                    color = tint, style = MaterialTheme.typography.labelMedium, letterSpacing = 0.sp)
            }
        }
    }
}

private fun inr(value: Double): String {
    val whole = abs(value).roundToLong().toString()
    val grouped = if (whole.length <= 3) whole else whole.dropLast(3).reversed().chunked(2)
        .joinToString(",").reversed() + "," + whole.takeLast(3)
    return "${if (value < 0) "-" else ""}\u20b9$grouped"
}

private fun signedInr(value: Double) = (if (value > 0) "+" else "") + inr(value)
private fun signed(value: Double) = if (!value.isFinite()) "N/A" else String.format(Locale.US, "%+.1f", value)
private fun months(value: Double) = if (value.isInfinite()) "Unlimited" else String.format(Locale.US, "%.1f mo", value)
private fun percent(value: Double) = String.format(Locale.US, "%.0f%%", value * 100)

@Preview(name = "What-if sheet", showBackground = true, widthDp = 390, heightDp = 844)
@Preview(name = "What-if sheet dark", showBackground = true, widthDp = 390, heightDp = 844,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WhatIfSheetPreview() {
    val period = CashFlowPeriod(85000.0, 24500.0, 26700.0, 12000.0, 10000.0)
    val inputs = remember {
        SimulationInputs(90000.0, 40000.0, List(6) { period },
            List(100) { i -> List(6) { period.copy(income = 55000.0 + i * 400) } },
            listOf(SimulationLoan(1850000.0, 0.085, 18500.0)))
    }
    WhatIfSheet(inputs, baselineShortfallRisk = 0.25, onDismissRequest = {}, onApplyToPlan = {},
        initialChange = WhatIfChange(discretionaryCut = 0.25))
}

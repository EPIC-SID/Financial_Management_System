package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.CanvasWhite
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.MutedInk
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.theme.SurfaceNeutral
import com.example.financeapp.R

enum class RupAiButtonVariant { PRIMARY, OUTLINE }
enum class RupAiButtonState { DEFAULT, PRESSED, FOCUSED, DISABLED, LOADING }
enum class RupAiButtonSize { COMPACT, DEFAULT, LARGE }

@Composable
fun RupAiButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RupAiButtonVariant = RupAiButtonVariant.PRIMARY,
    size: RupAiButtonSize = RupAiButtonSize.DEFAULT,
    state: RupAiButtonState = RupAiButtonState.DEFAULT,
    disabledReason: String? = null,
    loadingLabel: String = "Saving…"
) {
    val disabled = state == RupAiButtonState.DISABLED
    val loading = state == RupAiButtonState.LOADING
    require(!disabled || !disabledReason.isNullOrBlank()) { "Disabled buttons need a visible reason" }
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val focused by interactions.collectIsFocusedAsState()
    val emphasized = !disabled && !loading &&
        (pressed || focused || state == RupAiButtonState.PRESSED || state == RupAiButtonState.FOCUSED)
    val minimumHeight = when (size) {
        RupAiButtonSize.COMPACT -> Spacing.compactButtonHeight
        RupAiButtonSize.DEFAULT -> Spacing.defaultButtonHeight
        RupAiButtonSize.LARGE -> Spacing.largeButtonHeight
    }
    val verticalPadding = when (size) {
        RupAiButtonSize.COMPACT -> Spacing.xs
        RupAiButtonSize.DEFAULT -> Spacing.sm
        RupAiButtonSize.LARGE -> Spacing.md
    }
    val fill = if (variant == RupAiButtonVariant.PRIMARY) ActionGreen else CanvasWhite
    val content = if (variant == RupAiButtonVariant.PRIMARY) CanvasWhite else Ink
    val border = when {
        emphasized -> BorderStroke(2.dp, Ink)
        variant == RupAiButtonVariant.OUTLINE -> BorderStroke(1.dp, if (disabled) MutedInk else Ink)
        else -> null
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.button))) {
            if (variant == RupAiButtonVariant.PRIMARY && !disabled) {
                BrandImageBackground(R.drawable.bg_green, Modifier.fillMaxSize()) {}
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = minimumHeight).semantics {
                    if (loading) {
                        stateDescription = loadingLabel
                        liveRegion = LiveRegionMode.Polite
                    }
                },
                enabled = !disabled && !loading,
                interactionSource = interactions,
                shape = RoundedCornerShape(Radius.button),
                border = border,
                elevation = null,
                contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = verticalPadding),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (variant == RupAiButtonVariant.PRIMARY && !disabled) Color.Transparent else fill,
                    contentColor = content,
                    disabledContainerColor = if (loading) Color.Transparent else SurfaceNeutral,
                    disabledContentColor = if (loading) content else MutedInk
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp).clearAndSetSemantics { },
                        color = content,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(Spacing.xs))
                }
                Text(if (loading) loadingLabel else label, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (disabled) Text(disabledReason!!, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
    }
}

class RupAiButtonStatePreviewProvider : CollectionPreviewParameterProvider<RupAiButtonState>(RupAiButtonState.values().toList())

@Preview(name = "CTA · every state", widthDp = 320, showBackground = true)
@Preview(name = "CTA · every state · 130%", widthDp = 320, fontScale = 1.3f, showBackground = true)
@Preview(name = "CTA · every state · 200%", widthDp = 320, fontScale = 2f, showBackground = true)
@Composable
private fun RupAiButtonPreview(@PreviewParameter(RupAiButtonStatePreviewProvider::class) state: RupAiButtonState) {
    FinanceAppTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            RupAiButton("See plan", {}, state = state, disabledReason = "Add an account to enable this action.")
            RupAiButton("Explore demo", {}, variant = RupAiButtonVariant.OUTLINE, state = state,
                disabledReason = "Add an account to enable this action.")
        }
    }
}

@Preview(name = "CTA · every size", widthDp = 320, showBackground = true)
@Preview(name = "CTA · every size · 200%", widthDp = 320, fontScale = 2f, showBackground = true)
@Composable
private fun RupAiButtonSizesPreview() {
    FinanceAppTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            RupAiButton("Compact · 40 dp", {}, size = RupAiButtonSize.COMPACT)
            RupAiButton("Default · 48 dp", {}, size = RupAiButtonSize.DEFAULT)
            RupAiButton("Large · 56 dp", {}, size = RupAiButtonSize.LARGE)
        }
    }
}

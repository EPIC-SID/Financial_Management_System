package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.ActionGreen
import com.example.financeapp.ui.theme.Ink
import com.example.financeapp.ui.theme.Radius
import com.example.financeapp.ui.theme.Spacing

@Composable
fun <T> PlanSwitch(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().semantics { selectableGroup() }) {
        options.forEachIndexed { index, option ->
            Surface(
                modifier = Modifier.weight(1f).heightIn(min = Spacing.minimumTouchTarget).selectable(
                    selected = selected == option,
                    role = Role.RadioButton,
                    onClick = { onSelect(option) }
                ),
                shape = RoundedCornerShape(
                    topStart = if (index == 0) Radius.button else Spacing.xxs,
                    bottomStart = if (index == 0) Radius.button else Spacing.xxs,
                    topEnd = if (index == options.lastIndex) Radius.button else Spacing.xxs,
                    bottomEnd = if (index == options.lastIndex) Radius.button else Spacing.xxs
                ),
                color = if (selected == option) ActionGreen else MaterialTheme.colorScheme.surface,
                contentColor = Ink,
                border = BorderStroke(1.dp, Ink)
            ) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(vertical = Spacing.sm), contentAlignment = Alignment.Center) {
                    Text(label(option), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

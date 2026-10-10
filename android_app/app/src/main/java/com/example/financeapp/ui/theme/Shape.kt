package com.example.financeapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Radius {
    val tag = 8.dp
    val field = 12.dp
    val button = 16.dp
    val card = 20.dp
    val sheet = 24.dp
}

object Elevation {
    val base = 0.dp
    val raised = 0.dp
    val sheet = 0.dp
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Radius.tag),
    small = RoundedCornerShape(Radius.field),
    medium = RoundedCornerShape(Radius.button),
    large = RoundedCornerShape(Radius.card),
    extraLarge = RoundedCornerShape(Radius.sheet)
)

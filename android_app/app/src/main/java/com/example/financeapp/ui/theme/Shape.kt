package com.example.financeapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val TagRadius = RoundedCornerShape(6.dp)
val ButtonRadius = RoundedCornerShape(12.dp)
val CardRadius = RoundedCornerShape(16.dp)
val SheetRadius = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val PillRadius = RoundedCornerShape(percent = 50)

val AppShapes = Shapes(
    extraSmall = TagRadius,
    small = ButtonRadius,
    medium = CardRadius,
    large = CardRadius,
    extraLarge = RoundedCornerShape(24.dp),
)

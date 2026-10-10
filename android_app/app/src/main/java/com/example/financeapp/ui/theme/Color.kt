package com.example.financeapp.ui.theme

import androidx.compose.ui.graphics.Color

val CanvasWhite = Color(0xFFFFFFFF)
val SurfaceNeutral = Color(0xFFF6F7F5)
val Ink = Color(0xFF101010)
val MutedInk = Color(0xFF60665D)
// Shape-fill/border bindings were not retained in figma-source.json. Use the
// explicit ink override for outlines rather than treating an old hex as verified.
val OutlineNeutral = Ink
val ActionGreen = Color(0xFF52A145)
val ActionGreenPressed = ActionGreen
val OnAction = Ink
// Compatibility for existing imports; new components use OnAction.
val OnActionWhite = OnAction
// Guidance/highlights/closing band only. Never a status or text colour.
val GuidanceYellow = Color(0xFFFFC200)
val OnGuidance = Ink
val SummaryGreen = Color(0xFFE9F3E6)
val ErrorPink = Color(0xFFFBE1E7)
val ErrorInk = Color(0xFFA52A48)
// Orange implementation of the user override; these hexes are not Figma tokens.
val WatchWash = Color(0xFFFFE4CC)
val WatchInk = Color(0xFF9A4200)

// Keep existing screen imports working until their scheduled migration.
val BronzePrimary = ActionGreen
val BackgroundWhite = CanvasWhite
val SurfaceWhite = CanvasWhite
val TextBlack = Ink
val TextGray = MutedInk
val TealPositive = ActionGreen
val BorderBlack = OutlineNeutral
val SurfaceGray = SurfaceNeutral
val FoodColor = Ink
val GroceriesColor = Ink
val MiscColor = Ink

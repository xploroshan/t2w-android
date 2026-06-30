package com.taleson2wheels.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Mirrors the website's radius language: rounded-xl (12) for controls/inputs,
// rounded-2xl (16) for cards, with a pill for chips/badges via CircleShape at
// call sites. Material 3 maps: small→controls, medium/large→cards.
val T2WShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

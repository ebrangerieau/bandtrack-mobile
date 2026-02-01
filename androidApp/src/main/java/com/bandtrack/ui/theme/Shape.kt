package com.bandtrack.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Formes plus arrondies pour un aspect "Moderne/Soft"
val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp), // Pour les cartes
    large = RoundedCornerShape(24.dp)   // Pour les dialogs / bottom sheets
)

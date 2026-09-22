package com.example.ui.appleknowledge

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricCyanLight
import com.example.ui.theme.TechDarkCardElevated

/**
 * Keeps Apple Official Knowledge visually separate from deterministic diagnostic actions while
 * making the contextual module available from Home on Android.
 */
@Composable
fun AppleOfficialHomeEntry(
    onOpenAppleOfficial: () -> Unit,
    content: @Composable () -> Unit
) {
    Box {
        content()
        ExtendedFloatingActionButton(
            onClick = onOpenAppleOfficial,
            icon = { Icon(Icons.Default.Verified, contentDescription = null) },
            text = { Text("Apple Oficial") },
            containerColor = TechDarkCardElevated,
            contentColor = ElectricCyanLight,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(PaddingValues(end = 18.dp, bottom = 86.dp))
                .testTag("home_apple_official_entry")
        )
    }
}

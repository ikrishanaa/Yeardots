package com.krishana.onedot

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun check() {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = true,
            onClick = {},
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {}
        ) {
            Text("A")
        }
    }
}

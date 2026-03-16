package com.yamp.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.yamp.domain.model.SortField
import com.yamp.ui.theme.DarkSurfaceVariant
import com.yamp.ui.theme.Dimensions

@Composable
fun SortChipRow(
    selectedField: SortField,
    onFieldSelected: (SortField) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = Dimensions.paddingLarge, vertical = Dimensions.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.paddingMedium)
    ) {
        SortField.entries.forEach { field ->
            FilterChip(
                selected = field == selectedField,
                onClick = { onFieldSelected(field) },
                label = {
                    Text(
                        text = field.name.lowercase().replaceFirstChar { it.uppercase() }
                            .replace("_", " "),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = DarkSurfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

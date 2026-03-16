package com.yamp.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yamp.ui.theme.AccentCyan
import com.yamp.ui.theme.DarkCard
import com.yamp.ui.theme.DarkSurfaceVariant
import com.yamp.ui.theme.Dimensions
import com.yamp.ui.theme.DividerColor
import com.yamp.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimensions.paddingLarge)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = Dimensions.paddingLarge)
        )

        // Library Stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                Text("Library", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${state.trackCount} tracks total",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
                Text(
                    "${state.incompleteMetadataCount} tracks with incomplete metadata",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

        // Scan
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { viewModel.onRescanLibrary() },
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                Text("Rescan Library", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Scan device for new music files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

        // Metadata
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                Text("Metadata", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Auto-fetch metadata",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Look up missing artist, album, and genre info from MusicBrainz",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = state.autoFetchMetadata,
                        onCheckedChange = viewModel::onToggleAutoFetchMetadata,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = DarkSurfaceVariant,
                            uncheckedTrackColor = DividerColor
                        )
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = DividerColor
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onFetchMetadata() }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        "Fetch metadata now",
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentCyan
                    )
                    state.metadataFetchProgress?.let { progress ->
                        Text(
                            "Progress: $progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimensions.paddingLarge))

        // About
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(Dimensions.paddingLarge)) {
                Text("About", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("YAMP v1.0.0", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Yet Another Music Player",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

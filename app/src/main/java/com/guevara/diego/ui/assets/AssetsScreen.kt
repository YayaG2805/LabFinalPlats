package com.guevara.diego.ui.assets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guevara.diego.data.local.AssetEntity

@Composable
fun AssetsScreen(
    onClickAsset: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AssetsViewModel = viewModel(
        factory = AssetsViewModel.Factory(
            repository = com.guevara.diego.data.repository.AssetRepository(
                com.guevara.diego.data.local.AppDatabase.getInstance(context).assetDao()
            ),
            context = context
        )
    )

    val uiState by viewModel.uiState.collectAsState()
    val lastSavedDate by viewModel.lastSavedDate.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con título
        Text(
            text = "Criptomonedas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // Indicador de estado
        StatusIndicator(
            uiState = uiState,
            lastSavedDate = lastSavedDate
        )

        Spacer(Modifier.height(12.dp))

        // Botón para guardar offline
        Button(
            onClick = { viewModel.saveForOffline() },
            enabled = uiState is AssetsUiState.Success && (uiState as AssetsUiState.Success).isOnline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar para ver offline")
        }

        Spacer(Modifier.height(16.dp))

        // Contenido según el estado
        when (val state = uiState) {
            is AssetsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Cargando datos...")
                    }
                }
            }
            is AssetsUiState.Success -> {
                if (state.assets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay datos para mostrar",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.assets) { asset ->
                            AssetItem(
                                asset = asset,
                                onClick = { onClickAsset(asset.id) }
                            )
                        }
                    }
                }
            }
            is AssetsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAssets() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    uiState: AssetsUiState,
    lastSavedDate: String?
) {
    when (uiState) {
        is AssetsUiState.Success -> {
            if (uiState.isOnline) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(12.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Viendo data más reciente",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else if (lastSavedDate != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(12.dp)
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color(0xFFFFA726),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Viendo data del $lastSavedDate",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
fun AssetItem(
    asset: AssetEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = asset.symbol,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", asset.priceUsd)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                val change = asset.changePercent24Hr
                val changeColor = if (change >= 0.0) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFF44336)
                }

                val changeSymbol = if (change >= 0.0) "+" else ""

                Surface(
                    color = changeColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "$changeSymbol${String.format("%.2f", change)}%",
                        color = changeColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
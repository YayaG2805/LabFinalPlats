package com.guevara.diego.ui.detail

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guevara.diego.data.datastore.PreferencesManager
import com.guevara.diego.data.local.AppDatabase
import com.guevara.diego.data.local.AssetEntity
import com.guevara.diego.data.network.ApiClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val asset: AssetEntity, val isOnline: Boolean) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    context: Context,
    assetId: String,
    onBack: () -> Unit
) {
    val db = remember { AppDatabase.getInstance(context) }
    var uiState by remember { mutableStateOf<DetailUiState>(DetailUiState.Loading) }
    var lastSavedDate by remember { mutableStateOf<String?>(null) }

    // Leer última fecha guardada
    LaunchedEffect(Unit) {
        PreferencesManager.lastUpdateFlow(context).collect { value ->
            lastSavedDate = value
        }
    }

    // Cargar datos del asset
    LaunchedEffect(assetId) {
        try {
            // Intentar obtener del API
            val dto = ApiClient.getAsset(assetId)

            val mapped = AssetEntity(
                id = dto.id,
                name = dto.name,
                symbol = dto.symbol,
                priceUsd = dto.priceUsd.toDoubleOrNull() ?: 0.0,
                changePercent24Hr = dto.changePercent24Hr.toDoubleOrNull() ?: 0.0,
                supply = dto.supply?.toDoubleOrNull(),
                maxSupply = dto.maxSupply?.toDoubleOrNull(),
                marketCapUsd = dto.marketCapUsd?.toDoubleOrNull(),
                lastUpdated = LocalDateTime.now().toString()
            )

            // Actualizar en DB
            db.assetDao().insertAll(listOf(mapped))
            uiState = DetailUiState.Success(mapped, isOnline = true)
        } catch (e: Exception) {
            // Si falla, intentar cargar desde DB
            val local = db.assetDao().getById(assetId)
            if (local != null) {
                uiState = DetailUiState.Success(local, isOnline = false)
            } else {
                uiState = DetailUiState.Error("No se pudo cargar la información")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (val state = uiState) {
                        is DetailUiState.Success -> Text(state.asset.name)
                        else -> Text("Detalle")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Cargando información...")
                        }
                    }
                }
                is DetailUiState.Success -> {
                    DetailContent(
                        asset = state.asset,
                        isOnline = state.isOnline,
                        lastSavedDate = lastSavedDate
                    )
                }
                is DetailUiState.Error -> {
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
                            Button(onClick = onBack) {
                                Text("Regresar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailContent(
    asset: AssetEntity,
    isOnline: Boolean,
    lastSavedDate: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Indicador de estado
        if (isOnline) {
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

        Spacer(Modifier.height(24.dp))

        // Nombre y símbolo
        Text(
            text = asset.name,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = asset.symbol,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Precio y cambio
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Precio (USD)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$${String.format("%.2f", asset.priceUsd)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Cambio 24h",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                val change = asset.changePercent24Hr
                val changeColor = if (change >= 0.0) {
                    Color(0xFF4CAF50)
                } else {
                    Color(0xFFF44336)
                }
                val changeSymbol = if (change >= 0.0) "+" else ""

                Text(
                    text = "$changeSymbol${String.format("%.2f", change)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = changeColor
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Supply y Market Cap
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(
                    label = "Supply",
                    value = asset.supply?.let { formatNumber(it) } ?: "N/A"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow(
                    label = "Max Supply",
                    value = asset.maxSupply?.let { formatNumber(it) } ?: "N/A"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                DetailRow(
                    label = "Market Cap (USD)",
                    value = asset.marketCapUsd?.let { "$${formatNumber(it)}" } ?: "N/A"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Última actualización local
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Última actualización local",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(asset.lastUpdated),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun formatNumber(value: Double): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.2fB", value / 1_000_000_000)
        value >= 1_000_000 -> String.format("%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format("%.2fK", value / 1_000)
        else -> String.format("%.2f", value)
    }
}

fun formatTimestamp(timestamp: String): String {
    return try {
        val dateTime = LocalDateTime.parse(timestamp)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        dateTime.format(formatter)
    } catch (e: Exception) {
        timestamp
    }
}
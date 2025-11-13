package com.guevara.diego.ui.assets

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed class UiState {
    object Loading : UiState()
    data class Success(val isOnline: Boolean) : UiState()
    data class Error(val message: String, val hasLocalData: Boolean) : UiState()
}

@Composable
fun AssetsScreen(
    context: Context,
    onClickAsset: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    var assets by remember { mutableStateOf<List<AssetEntity>>(emptyList()) }
    var uiState by remember { mutableStateOf<UiState>(UiState.Loading) }
    var lastSavedDate by remember { mutableStateOf<String?>(null) }

    // Leer última fecha guardada desde DataStore
    LaunchedEffect(Unit) {
        PreferencesManager.lastUpdateFlow(context).collect { value ->
            lastSavedDate = value
        }
    }

    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        loadData(
            db = db,
            onSuccess = { loadedAssets, isOnline ->
                assets = loadedAssets
                uiState = UiState.Success(isOnline)
            },
            onError = { hasLocalData ->
                if (hasLocalData) {
                    scope.launch {
                        assets = db.assetDao().getAll()
                        uiState = UiState.Error("Sin conexión a internet", hasLocalData = true)
                    }
                } else {
                    uiState = UiState.Error("Sin conexión y sin datos guardados", hasLocalData = false)
                }
            }
        )
    }

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
            onClick = {
                scope.launch {
                    if (uiState is UiState.Success && (uiState as UiState.Success).isOnline) {
                        val now = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        val formattedDate = now.format(formatter)

                        PreferencesManager.saveLastUpdate(context, formattedDate)
                        lastSavedDate = formattedDate
                    }
                }
            },
            enabled = uiState is UiState.Success && (uiState as UiState.Success).isOnline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar para ver offline")
        }

        Spacer(Modifier.height(16.dp))

        // Contenido según el estado
        when (uiState) {
            is UiState.Loading -> {
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
            is UiState.Success, is UiState.Error -> {
                if (assets.isEmpty()) {
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
                        items(assets) { asset ->
                            AssetItem(
                                asset = asset,
                                onClick = { onClickAsset(asset.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(
    uiState: UiState,
    lastSavedDate: String?
) {
    when (uiState) {
        is UiState.Success -> {
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
                            modifier = Modifier
                                .size(12.dp)
                                .padding(end = 8.dp)
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
            }
        }
        is UiState.Error -> {
            if (uiState.hasLocalData && lastSavedDate != null) {
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
                            modifier = Modifier
                                .size(12.dp)
                                .padding(end = 8.dp)
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

suspend fun loadData(
    db: AppDatabase,
    onSuccess: (List<AssetEntity>, Boolean) -> Unit,
    onError: (Boolean) -> Unit
) {
    try {
        // Intentar obtener datos del API
        val remote = ApiClient.getAssets()

        val mapped = remote.map {
            AssetEntity(
                id = it.id,
                name = it.name,
                symbol = it.symbol,
                priceUsd = it.priceUsd.toDoubleOrNull() ?: 0.0,
                changePercent24Hr = it.changePercent24Hr.toDoubleOrNull() ?: 0.0,
                supply = it.supply?.toDoubleOrNull(),
                maxSupply = it.maxSupply?.toDoubleOrNull(),
                marketCapUsd = it.marketCapUsd?.toDoubleOrNull(),
                lastUpdated = LocalDateTime.now().toString()
            )
        }

        // Guardar en DB (cache temporal en memoria)
        db.assetDao().insertAll(mapped)
        val assets = db.assetDao().getAll()

        onSuccess(assets, true)
    } catch (e: Exception) {
        // Si hay error, intentar cargar desde DB
        val localAssets = db.assetDao().getAll()
        if (localAssets.isNotEmpty()) {
            onError(true)
        } else {
            onError(false)
        }
    }
}
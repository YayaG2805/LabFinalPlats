package com.guevara.diego.ui.assets

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guevara.diego.data.datastore.PreferencesManager
import com.guevara.diego.data.local.AppDatabase
import com.guevara.diego.data.local.AssetEntity
import com.guevara.diego.data.network.ApiClient
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun AssetsScreen(
    context: Context,
    onClickAsset: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }

    var assets by remember { mutableStateOf<List<AssetEntity>>(emptyList()) }
    var status by remember { mutableStateOf("Cargando data…") }
    var isOnline by remember { mutableStateOf(true) }
    var lastSaved by remember { mutableStateOf<String?>(null) }

    // Leer última fecha guardada (DataStore)
    LaunchedEffect(Unit) {
        PreferencesManager.lastUpdateFlow(context).collect { value ->
            lastSaved = value
        }
    }

    // Cargar data desde API o DB
    LaunchedEffect(Unit) {
        try {
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

            db.assetDao().insertAll(mapped)
            assets = db.assetDao().getAll()

            status = "Viendo data más reciente (en memoria)"
            isOnline = true
        } catch (e: Exception) {
            assets = db.assetDao().getAll()
            isOnline = false
            status = if (assets.isNotEmpty()) {
                "Sin internet. Viendo data almacenada del ${lastSaved ?: "?"}"
            } else {
                "Sin internet y sin data almacenada."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(text = status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                if (isOnline) {
                    scope.launch {
                        val now = LocalDateTime.now().toString()
                        PreferencesManager.saveLastUpdate(context, now)
                        status = "Data guardada para uso offline ($now)"
                    }
                }
            }
        ) {
            Text("Guardar para ver offline")
        }

        Spacer(Modifier.height(12.dp))

        if (assets.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay datos para mostrar.")
            }
        } else {
            LazyColumn {
                items(assets) { item ->
                    AssetItem(
                        asset = item,
                        onClick = { onClickAsset(item.id) }
                    )
                }
            }
        }
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
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(asset.name, style = MaterialTheme.typography.titleMedium)
            Text(asset.symbol, style = MaterialTheme.typography.bodySmall)

            Text(
                text = "USD ${asset.priceUsd}",
                style = MaterialTheme.typography.bodyMedium
            )

            val change = asset.changePercent24Hr
            val color =
                if (change >= 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

            Text(
                text = "24h: ${"%.2f".format(change)}%",
                color = color
            )
        }
    }
}

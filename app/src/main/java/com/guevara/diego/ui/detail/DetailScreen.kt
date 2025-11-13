package com.guevara.diego.ui.detail

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guevara.diego.data.local.AppDatabase
import com.guevara.diego.data.local.AssetEntity
import com.guevara.diego.data.network.ApiClient
import java.time.LocalDateTime

@Composable
fun DetailScreen(
    context: Context,
    assetId: String,
    onBack: () -> Unit
) {
    val db = remember { AppDatabase.getInstance(context) }

    var asset by remember { mutableStateOf<AssetEntity?>(null) }
    var status by remember { mutableStateOf("Cargando…") }

    LaunchedEffect(assetId) {
        try {
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

            db.assetDao().insertAll(listOf(mapped))
            asset = mapped
            status = "Viendo data más reciente"
        } catch (e: Exception) {
            val local = db.assetDao().getById(assetId)
            if (local != null) {
                asset = local
                status = "Sin internet. Viendo data almacenada del ${local.lastUpdated}"
            } else {
                status = "No hay datos disponibles."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(status, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        asset?.let {
            Text(it.name, style = MaterialTheme.typography.titleLarge)
            Text(it.symbol, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))

            Text("Precio (USD): ${it.priceUsd}")
            Text("Cambio 24h: ${"%.2f".format(it.changePercent24Hr)}%")
            Spacer(Modifier.height(8.dp))

            Text("Supply: ${it.supply ?: "?"}")
            Text("Max Supply: ${it.maxSupply ?: "?"}")
            Text("Market Cap: ${it.marketCapUsd ?: "?"}")
            Spacer(Modifier.height(8.dp))

            Text("Última actualización local:")
            Text(it.lastUpdated, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(onClick = onBack) {
            Text("Regresar")
        }
    }
}

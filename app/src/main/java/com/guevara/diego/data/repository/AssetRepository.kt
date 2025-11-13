package com.guevara.diego.data.repository

import com.guevara.diego.data.local.AssetDao
import com.guevara.diego.data.local.AssetEntity
import com.guevara.diego.data.network.ApiClient
import java.time.LocalDateTime

/**
 * Repository: Fuente única de verdad para los datos de Assets
 * Maneja la lógica de negocio entre API y BD local
 */
class AssetRepository(
    private val assetDao: AssetDao
) {

    /**
     * Obtiene lista de assets.
     * Prioridad: API → Cache local
     */
    suspend fun getAssets(): Result<List<AssetEntity>> {
        return try {
            // Intentar obtener del API
            val remoteAssets = ApiClient.getAssets()

            // Mapear a entidades
            val entities = remoteAssets.map { dto ->
                AssetEntity(
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
            }

            // Guardar en cache (memoria temporal)
            assetDao.insertAll(entities)

            Result.success(entities)
        } catch (e: Exception) {
            // Si falla el API, intentar cargar desde cache
            val cachedAssets = assetDao.getAll()
            if (cachedAssets.isNotEmpty()) {
                Result.success(cachedAssets)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene un asset específico por ID.
     * Prioridad: API → Cache local
     */
    suspend fun getAssetById(id: String): Result<AssetEntity> {
        return try {
            // Intentar obtener del API
            val dto = ApiClient.getAsset(id)

            val entity = AssetEntity(
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

            // Actualizar cache
            assetDao.insertAll(listOf(entity))

            Result.success(entity)
        } catch (e: Exception) {
            // Si falla, intentar desde cache
            val cachedAsset = assetDao.getById(id)
            if (cachedAsset != null) {
                Result.success(cachedAsset)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene assets desde cache local solamente
     */
    suspend fun getCachedAssets(): List<AssetEntity> {
        return assetDao.getAll()
    }

    /**
     * Obtiene un asset desde cache local solamente
     */
    suspend fun getCachedAssetById(id: String): AssetEntity? {
        return assetDao.getById(id)
    }

    /**
     * Persiste datos para uso offline
     */
    suspend fun saveForOffline(assets: List<AssetEntity>) {
        assetDao.insertAll(assets)
    }
}
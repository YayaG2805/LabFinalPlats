package com.guevara.diego.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val symbol: String,
    val priceUsd: Double,
    val changePercent24Hr: Double,
    val supply: Double?,
    val maxSupply: Double?,
    val marketCapUsd: Double?,
    val lastUpdated: String
)

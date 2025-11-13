package com.guevara.diego.navigation

import kotlinx.serialization.Serializable

/**
 * Destinations type-safe usando @Serializable
 * Siguiendo el patrón del proyecto HusL
 */

@Serializable
object AssetsListDestination

@Serializable
data class AssetDetailDestination(
    val assetId: String
)
package com.guevara.diego.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object ApiClient {

    // Base de la API que te dio tu profe
    private const val BASE_URL = "https://rest.coincap.io/v3/"
    private const val API_KEY =
        "6f8c2f757cc81e9950a05aeed8292abff853114ebc731977f3f5a580b1e9371a"

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }

        // Header de autorización
        defaultRequest {
            header("Authorization", "Bearer $API_KEY")
        }
    }

    // GET /assets → lista
    suspend fun getAssets(): List<AssetDto> {
        val response: AssetsResponse = client
            .get("${BASE_URL}assets")
            .body()
        return response.data
    }

    // GET /assets/{id} → detalle
    suspend fun getAsset(id: String): AssetDto {
        val response: AssetResponse = client
            .get("${BASE_URL}assets/$id")
            .body()
        return response.data
    }
}

@Serializable
data class AssetsResponse(
    val data: List<AssetDto>
)

@Serializable
data class AssetResponse(
    val data: AssetDto
)

@Serializable
data class AssetDto(
    val id: String,
    val rank: String,
    val symbol: String,
    val name: String,
    val supply: String? = null,
    val maxSupply: String? = null,
    val marketCapUsd: String? = null,
    val volumeUsd24Hr: String? = null,
    val priceUsd: String,
    val changePercent24Hr: String,
)

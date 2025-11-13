package com.guevara.diego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.guevara.diego.navigation.AssetDetailDestination
import com.guevara.diego.navigation.AssetsListDestination
import com.guevara.diego.ui.assets.AssetsScreen
import com.guevara.diego.ui.detail.DetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = AssetsListDestination
                    ) {
                        // Pantalla de lista de assets
                        composable<AssetsListDestination> {
                            AssetsScreen(
                                context = this@MainActivity,
                                onClickAsset = { assetId ->
                                    navController.navigate(
                                        AssetDetailDestination(assetId = assetId)
                                    )
                                }
                            )
                        }

                        // Pantalla de detalle de asset
                        composable<AssetDetailDestination> { backStackEntry ->
                            val destination = backStackEntry.toRoute<AssetDetailDestination>()

                            DetailScreen(
                                context = this@MainActivity,
                                assetId = destination.assetId,
                                onBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.guevara.diego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.guevara.diego.ui.assets.AssetsScreen
import com.guevara.diego.ui.detail.DetailScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var selectedAssetId by remember { mutableStateOf<String?>(null) }

            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {

                    if (selectedAssetId == null) {
                        AssetsScreen(
                            context = this,
                            onClickAsset = { id ->
                                selectedAssetId = id
                            }
                        )
                    } else {
                        DetailScreen(
                            context = this,
                            assetId = selectedAssetId!!,
                            onBack = { selectedAssetId = null }
                        )
                    }
                }
            }
        }
    }
}

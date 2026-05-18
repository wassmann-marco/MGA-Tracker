package org.ausbildungstracker.project

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MgaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.app = this
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            try {
                val inhalt = contentResolver.openInputStream(uri)?.use { it.reader(Charsets.UTF_8).readText() }
                if (inhalt != null) ActivityHolder.importCallback?.invoke(inhalt)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ActivityHolder.importLauncher = importLauncher

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

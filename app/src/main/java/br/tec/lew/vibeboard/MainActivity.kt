package br.tec.lew.vibeboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import br.tec.lew.vibeboard.ui.theme.VibeboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeboardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SetupScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

internal fun hasRecordAudioPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

internal fun isPackageImeEnabled(context: Context, packageName: String = context.packageName): Boolean {
    val imm = context.getSystemService(InputMethodManager::class.java) ?: return false
    return imm.enabledInputMethodList.any { it.packageName == packageName }
}

/**
 * [Settings.Secure.DEFAULT_INPUT_METHOD] is a component id: `package/class`.
 * Match by package so renames of the service class still work.
 */
internal fun isInputMethodIdForPackage(inputMethodId: String?, packageName: String): Boolean {
    if (inputMethodId.isNullOrEmpty() || packageName.isEmpty()) return false
    return inputMethodId.startsWith("$packageName/")
}

internal fun isPackageImeSelected(context: Context, packageName: String = context.packageName): Boolean {
    val current = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return isInputMethodIdForPackage(current, packageName)
}

@Composable
fun SetupScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    var hasPermission by remember { mutableStateOf(hasRecordAudioPermission(context)) }
    var imeEnabled by remember { mutableStateOf(isPackageImeEnabled(context)) }
    var imeSelected by remember { mutableStateOf(isPackageImeSelected(context)) }

    fun refreshSetupState() {
        hasPermission = hasRecordAudioPermission(context)
        imeEnabled = isPackageImeEnabled(context)
        imeSelected = isPackageImeSelected(context)
    }

    // Re-read system state whenever the user returns from Settings / the permission dialog.
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshSetupState()
            }
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        // IME flags may also have changed while the permission sheet was up.
        refreshSetupState()
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Vibeboard Setup", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        if (!hasPermission) {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant Microphone Permission")
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Text(
                text = "Microphone permission granted",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(onClick = {
            try {
                context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            } catch (e: Exception) {
                reportError(
                    e,
                    mapOf("action" to "startActivity", "intent" to Settings.ACTION_INPUT_METHOD_SETTINGS)
                )
            }
        }) {
            Text(
                if (imeEnabled) {
                    "1. Vibeboard enabled — open Settings"
                } else {
                    "1. Enable Vibeboard in Settings"
                }
            )
        }
        if (imeEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Input method is enabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            try {
                val imm = context.getSystemService(InputMethodManager::class.java)
                imm?.showInputMethodPicker()
            } catch (e: Exception) {
                reportError(e, mapOf("action" to "showInputMethodPicker"))
            }
        }) {
            Text(
                if (imeSelected) {
                    "2. Vibeboard selected — switch IME"
                } else {
                    "2. Select Vibeboard as Input Method"
                }
            )
        }
        if (imeSelected) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Vibeboard is the current input method",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (hasPermission && imeEnabled && imeSelected) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Setup complete — open any text field to use Vibeboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

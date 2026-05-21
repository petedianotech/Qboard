package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.keyboard.KeyboardPreferences
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val prefs = remember { KeyboardPreferences.getInstance(this) }
      val themeState by prefs.themeColor.collectAsState()
      
      val isDarkTheme = when (themeState) {
          1 -> true
          2 -> false
          else -> androidx.compose.foundation.isSystemInDarkTheme()
      }
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          SettingsScreen(
            modifier = Modifier.padding(innerPadding),
            prefs = prefs,
            themeState = themeState
          )
        }
      }
    }
  }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    prefs: KeyboardPreferences,
    themeState: Int
) {
    val context = LocalContext.current
    var isKeyboardEnabled by remember {
        mutableStateOf(isKeyboardEnabled(context))
    }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Qboard Settings", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Step 1: Enable Keyboard", style = MaterialTheme.typography.titleMedium)
                Text("You must enable Qboard in system settings and select it to begin.", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                    context.startActivity(intent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to System Settings")
                }
            }
        }

        val keyboardLayout by prefs.keyboardLayout.collectAsState()
        val predictionEnabled by prefs.predictionEnabled.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Layout & Customize", style = MaterialTheme.typography.titleMedium)
                
                Text("Physical Layout", style = MaterialTheme.typography.bodyMedium)
                segmentControl(
                    selectedIndex = keyboardLayout,
                    options = listOf("QWERTY", "AZERTY", "QWERTZ"),
                    onOptionSelected = { prefs.setKeyboardLayout(it) }
                )

                Spacer(modifier = Modifier.height(4.dp))
                PreferenceSwitch("Show Prediction Strip", predictionEnabled) { prefs.setPredictionEnabled(it) }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                segmentControl(
                    selectedIndex = themeState,
                    options = listOf("Auto", "Dark", "Light"),
                    onOptionSelected = { prefs.setThemeColor(it) }
                )
            }
        }

        val vibrationEnabled by prefs.vibrationEnabled.collectAsState()
        val soundEnabled by prefs.soundEnabled.collectAsState()
        val autoCapsEnabled by prefs.autoCapsEnabled.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Preferences", style = MaterialTheme.typography.titleMedium)
                PreferenceSwitch("Haptic Feedback (Vibration)", vibrationEnabled) { prefs.setVibrationEnabled(it) }
                PreferenceSwitch("Sound on Keypress", soundEnabled) { prefs.setSoundEnabled(it) }
                PreferenceSwitch("Auto-Capitalization", autoCapsEnabled) { prefs.setAutoCapsEnabled(it) }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Step 2: Start Typing!", style = MaterialTheme.typography.titleMedium)
                Text("Try typing below to see Qboard in action.", style = MaterialTheme.typography.bodyMedium)
                var text by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Tap to type...") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
            }
        }
    }
}

@Composable
fun PreferenceSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun segmentControl(selectedIndex: Int, options: List<String>, onOptionSelected: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = selectedIndex == index,
                onClick = { onOptionSelected(index) },
                label = { Text(option) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun isKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledMethods = imm.enabledInputMethodList
    return enabledMethods.any { it.packageName == context.packageName }
}


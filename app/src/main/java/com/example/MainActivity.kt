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
import androidx.compose.foundation.background

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val prefs = remember { KeyboardPreferences.getInstance(this) }
      val themeState by prefs.themeColor.collectAsState()
      val themeAccent by prefs.themeAccent.collectAsState()
      
      val isDarkTheme = when (themeState) {
          1 -> true
          2 -> false
          else -> androidx.compose.foundation.isSystemInDarkTheme()
      }
      
      MyApplicationTheme(darkTheme = isDarkTheme, accentIndex = themeAccent) {
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
    var isKeyboardSelected by remember {
        mutableStateOf(isKeyboardSelected(context))
    }
    val scrollState = rememberScrollState()

    // Periodically auto-recheck when returning from MainActivity
    LaunchedEffect(Unit) {
        while (true) {
            isKeyboardEnabled = isKeyboardEnabled(context)
            isKeyboardSelected = isKeyboardSelected(context)
            kotlinx.coroutines.delay(1500)
        }
    }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Step 1: Enable & Select Qboard", style = MaterialTheme.typography.titleMedium)
                
                // Status Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("1. Enabled in Settings:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    if (isKeyboardEnabled) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Enabled 🎉") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Disabled ❌") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("2. Selected as Active:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.weight(1f))
                    if (isKeyboardSelected) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Selected ✅") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Not Active ⏳") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Onboarding Action Buttons
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    }, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Go to System Settings")
                }

                Button(
                    onClick = {
                        isKeyboardEnabled = isKeyboardEnabled(context)
                        isKeyboardSelected = isKeyboardSelected(context)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isKeyboardEnabled && isKeyboardSelected) 
                            MaterialTheme.colorScheme.secondary 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm Keyboard is Enabled")
                }

                Button(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isKeyboardEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isKeyboardSelected) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.tertiary,
                        contentColor = if (isKeyboardSelected) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Text(if (isKeyboardSelected) "Already Active (Switch Preference)" else "Switch to Qboard")
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
                
                val keyboardHeight by prefs.keyboardHeight.collectAsState()
                Text("Keyboard Height: ${(keyboardHeight * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = keyboardHeight,
                    onValueChange = { prefs.setKeyboardHeight(it) },
                    valueRange = 0.5f..1.5f,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
                PreferenceSwitch("Show Prediction Strip", predictionEnabled) { prefs.setPredictionEnabled(it) }
            }
        }

        val themeAccent by prefs.themeAccent.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Theme Mode", style = MaterialTheme.typography.titleMedium)
                segmentControl(
                    selectedIndex = themeState,
                    options = listOf("Auto", "Dark", "Light"),
                    onOptionSelected = { prefs.setThemeColor(it) }
                )
                
                Spacer(
                    modifier = Modifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                )
                
                Text("Creative Accent Color", style = MaterialTheme.typography.titleMedium)
                segmentControl(
                    selectedIndex = themeAccent,
                    options = listOf("Blue", "Orange", "Green", "Red", "Purple"),
                    onOptionSelected = { prefs.setThemeAccent(it) }
                )
            }
        }

        val vibrationEnabled by prefs.vibrationEnabled.collectAsState()
        val soundEnabled by prefs.soundEnabled.collectAsState()
        val autoCapsEnabled by prefs.autoCapsEnabled.collectAsState()
        val cerebrasApiKey by prefs.cerebrasApiKey.collectAsState()
        val aiMood by prefs.aiMood.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("AI Assistant Settings", style = MaterialTheme.typography.titleMedium)
                var apiKeyLocal by remember { mutableStateOf(cerebrasApiKey) }
                LaunchedEffect(cerebrasApiKey) {
                    if (cerebrasApiKey != apiKeyLocal) {
                        apiKeyLocal = cerebrasApiKey
                    }
                }
                OutlinedTextField(
                    value = apiKeyLocal,
                    onValueChange = {
                        apiKeyLocal = it
                        prefs.setCerebrasApiKey(it)
                    },
                    label = { Text("Cerebras API Key") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("AI Tone", style = MaterialTheme.typography.bodyMedium)
                segmentControl(
                    selectedIndex = aiMood,
                    options = listOf("Normal", "Pro", "Casual", "Friendly"),
                    onOptionSelected = { prefs.setAiMood(it) }
                )
                Text("An AI button will appear on the keyboard top bar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

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
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledMethods = imm?.enabledInputMethodList ?: emptyList()
        enabledMethods.any { it.packageName == context.packageName }
    } catch (e: Exception) {
        false
    }
}

private fun isKeyboardSelected(context: Context): Boolean {
    return try {
        val defaultId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        defaultId != null && defaultId.contains(context.packageName)
    } catch (e: Exception) {
        false
    }
}


package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.ui.theme.MyApplicationTheme

class ComposeIME : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@ComposeIME)
            setViewTreeViewModelStoreOwner(this@ComposeIME)
            setViewTreeSavedStateRegistryOwner(this@ComposeIME)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@ComposeIME))
            
            setContent {
                val prefs = remember { KeyboardPreferences.getInstance(this@ComposeIME) }
                val themeState by prefs.themeColor.collectAsState()
                
                val isDarkTheme = when (themeState) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                }

                MyApplicationTheme(darkTheme = isDarkTheme) {
                    KeyboardView(
                        prefs = prefs,
                        onKeyPressed = { handleKey(it) },
                        onDeletePressed = { handleDelete() },
                        onEnterPressed = { handleEnter() },
                        onSpacePressed = { handleSpace() }
                    )
                }
            }
        }
    }

    private fun handleKey(key: String) {
        currentInputConnection?.commitText(key, 1)
    }
    
    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }
    
    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }
    
    private fun handleSpace() {
        currentInputConnection?.commitText(" ", 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

@Composable
fun KeyboardView(
    prefs: KeyboardPreferences,
    onKeyPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onEnterPressed: () -> Unit,
    onSpacePressed: () -> Unit,
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbols by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    
    val vibrationEnabled by prefs.vibrationEnabled.collectAsState()
    val soundEnabled by prefs.soundEnabled.collectAsState()
    val autoCapsEnabled by prefs.autoCapsEnabled.collectAsState()

    val onCheckCaps: () -> Boolean = {
        val ic = (context as? InputMethodService)?.currentInputConnection
        val mode = ic?.getCursorCapsMode(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        (mode ?: 0) > 0
    }

    val keysQWERTY = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("123", ",", "SPACE", ".", "ENTER")
    )

    val keysSymbols = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        listOf("ABC", "*", "\"", "'", ":", ";", "!", "?", "DEL"),
        listOf("123", ",", "SPACE", ".", "ENTER") 
    )

    val keys = if (isSymbols) keysSymbols else keysQWERTY

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp)
            .padding(bottom = 8.dp) 
    ) {
        for (row in keys) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (key in row) {
                    val weight = when(key) {
                        "SPACE" -> 4f
                        "SHIFT", "DEL", "ENTER", "123", "ABC" -> 1.5f
                        else -> 1f
                    }
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(54.dp)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (key) {
                                    "ENTER" -> MaterialTheme.colorScheme.primary
                                    "SHIFT", "DEL", "123", "ABC" -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            )
                            .pointerInput(key) {
                                detectTapGestures(
                                    onPress = {
                                        if (vibrationEnabled) {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        if (soundEnabled) {
                                            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
                                        }
                                        when (key) {
                                            "DEL" -> {
                                                onDeletePressed()
                                                if (autoCapsEnabled) isShifted = onCheckCaps()
                                            }
                                            "SPACE" -> {
                                                onSpacePressed()
                                                if (autoCapsEnabled) isShifted = onCheckCaps()
                                            }
                                            "ENTER" -> {
                                                onEnterPressed()
                                                if (autoCapsEnabled) isShifted = onCheckCaps()
                                            }
                                            "SHIFT" -> isShifted = !isShifted
                                            "123" -> isSymbols = true
                                            "ABC" -> isSymbols = false
                                            else -> {
                                                val output = if (isShifted && !isSymbols) key.uppercase() else key
                                                onKeyPressed(output)
                                                if (isShifted) {
                                                    isShifted = false // Turn off shift instantly after standard key
                                                } else if (autoCapsEnabled) {
                                                    isShifted = onCheckCaps()
                                                }
                                            }
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (key) {
                                "DEL" -> "⌫"
                                "SHIFT" -> "⇧"
                                "ENTER" -> "↩"
                                "SPACE" -> "␣"
                                "123" -> "?123"
                                else -> if (isShifted && !isSymbols) key.uppercase() else key
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (key == "ENTER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

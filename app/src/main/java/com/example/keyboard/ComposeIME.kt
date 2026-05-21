package com.example.keyboard

import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.ui.theme.MyApplicationTheme

private val vocabulary = listOf(
    "the", "be", "to", "of", "and", "a", "in", "that", "have", "I",
    "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
    "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
    "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
    "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
    "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
    "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
    "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
    "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
    "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
    "android", "keyboard", "compose", "qboard", "typing", "swift", "github", "release",
    "application", "development", "project", "custom", "theme", "prediction", "vibration",
    "sound", "feedback", "perfect", "awesome", "fast", "reliable", "beautiful", "modern"
)

private val nextWordPredictions = mapOf(
    "android" to listOf("app", "development", "keyboard", "device", "studio"),
    "keyboard" to listOf("layout", "settings", "theme", "prediction", "sound"),
    "beautiful" to listOf("theme", "modern", "reliable", "design", "keyboard"),
    "modern" to listOf("beautiful", "reliable", "keyboard", "application", "design"),
    "the" to listOf("application", "keyboard", "project", "vibration", "sound"),
    "i" to listOf("want", "think", "say", "know", "go"),
    "you" to listOf("can", "like", "do", "think", "want"),
    "to" to listOf("be", "have", "do", "say", "go"),
    "and" to listOf("reliable", "fast", "modern", "beautiful", "easy"),
    "prediction" to listOf("strip", "accuracy", "algorithm", "of", "settings"),
    "vibration" to listOf("feedback", "intensity", "on", "duration", "and"),
    "sound" to listOf("feedback", "effects", "volume", "on", "enabled"),
    "cerebras" to listOf("api", "key", "model", "response", "integration")
)

class ComposeInputViewOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun start() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

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

    // State to hold the active composing prefix and last completed word for predictions
    private var composingPrefix by mutableStateOf("")
    private var lastCompletedWord by mutableStateOf("")
    private var currentOwner: ComposeInputViewOwner? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        currentOwner?.start()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        currentOwner?.stop()
    }

    override fun onCreateInputView(): View {
        currentOwner?.destroy()
        val owner = ComposeInputViewOwner()
        currentOwner = owner

        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            
            setContent {
                val prefs = remember { KeyboardPreferences.getInstance(this@ComposeIME) }
                val themeState by prefs.themeColor.collectAsState()
                val themeAccent by prefs.themeAccent.collectAsState()
                
                val isDarkTheme = when (themeState) {
                    1 -> true
                    2 -> false
                    else -> isSystemInDarkTheme()
                }

                MyApplicationTheme(darkTheme = isDarkTheme, accentIndex = themeAccent) {
                    KeyboardView(
                        prefs = prefs,
                        composingPrefix = composingPrefix,
                        lastCompletedWord = lastCompletedWord,
                        onKeyPressed = { handleKey(it) },
                        onDeletePressed = { handleDelete() },
                        onEnterPressed = { handleEnter() },
                        onSpacePressed = { handleSpace() },
                        onSuggestionSelected = { handleSuggestionSelected(it) }
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateComposingPrefix()
    }

    override fun onUpdateSelection(
        oldSelStart: Int, oldSelEnd: Int,
        newSelStart: Int, newSelEnd: Int,
        candidatesStart: Int, candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateComposingPrefix()
    }

    private fun handleKey(key: String) {
        currentInputConnection?.commitText(key, 1)
        updateComposingPrefix()
    }
    
    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateComposingPrefix()
    }
    
    private fun handleEnter() {
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        updateComposingPrefix()
    }
    
    private fun handleSpace() {
        currentInputConnection?.commitText(" ", 1)
        updateComposingPrefix()
    }

    private fun updateComposingPrefix() {
        val ic = currentInputConnection
        val textBefore = ic?.getTextBeforeCursor(50, 0)?.toString() ?: ""
        composingPrefix = getLastWord(textBefore)
        lastCompletedWord = getLastCompletedWord(textBefore)
    }

    private fun getLastWord(text: String): String {
        if (text.isEmpty()) return ""
        val lastWordRegex = Regex("[a-zA-Z0-9']+$")
        val match = lastWordRegex.find(text)
        return match?.value ?: ""
    }

    private fun getLastCompletedWord(text: String): String {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return ""
        val lastWordRegex = Regex("[a-zA-Z0-9']+$")
        val match = lastWordRegex.find(trimmed)
        return match?.value ?: ""
    }

    private fun handleSuggestionSelected(suggestion: String) {
        val ic = currentInputConnection ?: return
        val prefixLength = composingPrefix.length
        if (prefixLength > 0) {
            ic.deleteSurroundingText(prefixLength, 0)
        }
        val casedSuggestion = matchCasing(composingPrefix, suggestion)
        ic.commitText("$casedSuggestion ", 1)
        updateComposingPrefix()
    }

    fun replaceText(newText: String) {
        val ic = currentInputConnection ?: return
        val request = android.view.inputmethod.ExtractedTextRequest()
        val textLength = ic.getExtractedText(request, 0)?.text?.length ?: 0
        ic.deleteSurroundingText(textLength, textLength)
        ic.commitText(newText, 1)
        updateComposingPrefix()
    }

    private fun matchCasing(prefix: String, word: String): String {
        if (prefix.isEmpty()) return word
        if (prefix[0].isUpperCase()) {
            if (prefix.all { it.isUpperCase() } && prefix.length > 1) {
                return word.uppercase()
            }
            return word.replaceFirstChar { it.uppercase() }
        }
        return word.lowercase()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        currentOwner?.destroy()
        currentOwner = null
        store.clear()
    }
}

@Composable
fun KeyboardView(
    prefs: KeyboardPreferences,
    composingPrefix: String,
    lastCompletedWord: String,
    onKeyPressed: (String) -> Unit,
    onDeletePressed: () -> Unit,
    onEnterPressed: () -> Unit,
    onSpacePressed: () -> Unit,
    onSuggestionSelected: (String) -> Unit
) {
    var isShifted by remember { mutableStateOf(false) }
    var isSymbols by remember { mutableStateOf(false) }
    var isEmoji by remember { mutableStateOf(false) }
    var isAILoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val view = LocalView.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val coroutineScope = rememberCoroutineScope()
    
    val vibrationEnabled by prefs.vibrationEnabled.collectAsState()
    val soundEnabled by prefs.soundEnabled.collectAsState()
    val autoCapsEnabled by prefs.autoCapsEnabled.collectAsState()
    val keyboardLayout by prefs.keyboardLayout.collectAsState()
    val predictionEnabled by prefs.predictionEnabled.collectAsState()
    val cerebrasApiKey by prefs.cerebrasApiKey.collectAsState()
    val aiMood by prefs.aiMood.collectAsState()
    val keyboardHeight by prefs.keyboardHeight.collectAsState()

    val onCheckCaps: () -> Boolean = {
        val ic = context.findIME()?.currentInputConnection
        val mode = ic?.getCursorCapsMode(android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        (mode ?: 0) > 0
    }

    val handleAIPress: () -> Unit = {
        val ime = context.findIME()
        val requestText = ime?.currentInputConnection?.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)?.text?.toString()
        if (!requestText.isNullOrBlank() && cerebrasApiKey.isNotBlank()) {
            isAILoading = true
            coroutineScope.launch {
                val service = CerebrasApiService()
                val response = service.rewriteText(requestText, cerebrasApiKey, aiMood)
                isAILoading = false
                if (response != null && !response.startsWith("Error:")) {
                    ime.replaceText(response)
                }
            }
        }
    }

    val keysQWERTY = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("123", ",", "EMOJI", "SPACE", ".", "ENTER")
    )

    val keysAZERTY = listOf(
        listOf("a", "z", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("q", "s", "d", "f", "g", "h", "j", "k", "l", "m"),
        listOf("SHIFT", "w", "x", "c", "v", "b", "n", ",", "DEL"),
        listOf("123", "EMOJI", "SPACE", "?", "ENTER")
    )

    val keysQWERTZ = listOf(
        listOf("q", "w", "e", "r", "t", "z", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "y", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("123", ",", "EMOJI", "SPACE", ".", "ENTER")
    )

    val keysSelected = when (keyboardLayout) {
        1 -> keysAZERTY
        2 -> keysQWERTZ
        else -> keysQWERTY
    }

    val keysSymbols = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
        listOf("ABC", "*", "\"", "'", ":", ";", "!", "?", "DEL"),
        listOf("123", ",", "EMOJI", "SPACE", ".", "ENTER") 
    )

    val keys = if (isSymbols) keysSymbols else keysSelected

    val predictions = remember(composingPrefix, lastCompletedWord, predictionEnabled) {
        if (!predictionEnabled) {
            emptyList()
        } else {
            val prefix = composingPrefix.lowercase()
            if (prefix.isEmpty()) {
                val completed = lastCompletedWord.lowercase()
                nextWordPredictions[completed] ?: listOf("the", "I", "you", "to", "and")
            } else {
                vocabulary.filter { it.startsWith(prefix) }.take(5)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(4.dp)
            .padding(bottom = 8.dp) 
    ) {
        // Predictions Strip
        if (predictionEnabled || cerebrasApiKey.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cerebrasApiKey.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp)
                            .clickable { handleAIPress() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAILoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "✨ AI",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.35f)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp)
                        .clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            if (clipboard.hasPrimaryClip()) {
                                val pasteData = clipboard.primaryClip?.getItemAt(0)?.text
                                if (pasteData != null) {
                                    context.findIME()?.currentInputConnection?.commitText(pasteData, 1)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📋", style = MaterialTheme.typography.bodyLarge)
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.35f)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )

                if (predictionEnabled && predictions.isNotEmpty()) {
                    for (suggestion in predictions) {
                        val casedOpt = remember(composingPrefix, suggestion) {
                            if (composingPrefix.isNotEmpty() && composingPrefix[0].isUpperCase()) {
                                if (composingPrefix.all { it.isUpperCase() } && composingPrefix.length > 1) {
                                    suggestion.uppercase()
                                } else {
                                    suggestion.replaceFirstChar { it.uppercase() }
                                }
                            } else {
                                suggestion
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSuggestionSelected(casedOpt) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = casedOpt,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (suggestion != predictions.last()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.35f)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            )
                        }
                    }
                } else if (predictionEnabled && predictions.isEmpty()) {
                     Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Keyboard Keys
        if (isEmoji) {
            val emojis = listOf("😀", "😂", "🥺", "😭", "😍", "🙏", "😊", "✨", "❤️", "👍", "🔥", "🥰", "🎉", "💯", "🤔", "🙌", "😘", "😎", "💪", "🤩", "😁", "😅", "😆", "😇", "😉", "😋", "😜", "🤪", "😝", "🤑", "🤗", "🤭", "🤫", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯")
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxWidth()
                    .height((54 * 4 * keyboardHeight).dp)
            ) {
                items(emojis.size) { index ->
                    Box(
                        modifier = Modifier
                            .height((48 * keyboardHeight).dp)
                            .clickable {
                                onKeyPressed(emojis[index])
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emojis[index], fontSize = 24.sp)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height((54 * keyboardHeight).dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { isEmoji = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("ABC", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .height((54 * keyboardHeight).dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onSpacePressed() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("␣", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height((54 * keyboardHeight).dp)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onDeletePressed() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌫", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        } else {
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
                                .height((54 * keyboardHeight).dp)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when (key) {
                                        "ENTER" -> MaterialTheme.colorScheme.primary
                                        "SHIFT", "DEL", "123", "ABC", "EMOJI" -> MaterialTheme.colorScheme.surfaceVariant
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
                                                "EMOJI" -> isEmoji = true
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
                                    "EMOJI" -> "😊"
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
}

private fun android.content.Context.findIME(): ComposeIME? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is ComposeIME) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? ComposeIME
}

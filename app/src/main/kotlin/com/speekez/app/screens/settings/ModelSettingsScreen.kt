package com.speekez.app.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speekez.api.ApiRouterManager
import com.speekez.app.LocalSnackbarHostState
import com.speekez.core.ApiMode
import com.speekez.core.ModelTier
import com.speekez.core.NetworkUtils
import com.speekez.security.EncryptedPreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { EncryptedPreferencesManager(context) }
    val apiRouter = remember { ApiRouterManager(context, prefs) }

    var apiMode by remember { mutableStateOf(prefs.getApiMode()) }
    var modelTier by remember { mutableStateOf(prefs.getModelTier()) }

    var openRouterKey by remember { mutableStateOf(prefs.getOpenRouterKey() ?: "") }
    var groqKey by remember { mutableStateOf(prefs.getGroqKey() ?: "") }
    var openAiKey by remember { mutableStateOf(prefs.getOpenAiKey() ?: "") }
    var anthropicKey by remember { mutableStateOf(prefs.getAnthropicKey() ?: "") }

    var customSttModel by remember { mutableStateOf(prefs.getCustomSttModel() ?: "") }
    var customRefinementModel by remember { mutableStateOf(prefs.getCustomRefinementModel() ?: "") }

    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showGroqKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var showAnthropicKey by remember { mutableStateOf(false) }

    var isTestingApi by remember { mutableStateOf(false) }
    var showSwitchModeDialog by remember { mutableStateOf<ApiMode?>(null) }

    val snackbarHostState = LocalSnackbarHostState.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Provider Mode",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            SegmentedButton(
                selected = apiMode == ApiMode.OPENROUTER,
                onClick = {
                    if (apiMode == ApiMode.SEPARATE && (openAiKey.isNotEmpty() || anthropicKey.isNotEmpty())) {
                        showSwitchModeDialog = ApiMode.OPENROUTER
                    } else {
                        apiMode = ApiMode.OPENROUTER
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("OpenRouter")
            }
            SegmentedButton(
                selected = apiMode == ApiMode.SEPARATE,
                onClick = {
                    if (apiMode == ApiMode.OPENROUTER && openRouterKey.isNotEmpty()) {
                        showSwitchModeDialog = ApiMode.SEPARATE
                    } else {
                        apiMode = ApiMode.SEPARATE
                    }
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Separate Keys")
            }
        }

        if (apiMode == ApiMode.OPENROUTER) {
            ApiKeyField(
                label = "OpenRouter API Key",
                value = openRouterKey,
                onValueChange = { openRouterKey = it },
                isVisible = showOpenRouterKey,
                onToggleVisibility = { showOpenRouterKey = !showOpenRouterKey },
                prefix = "sk-or-",
                isValid = openRouterKey.startsWith("sk-or-")
            )
            Spacer(modifier = Modifier.height(16.dp))
            ApiKeyField(
                label = "Groq API Key (Fast Transcription)",
                value = groqKey,
                onValueChange = { groqKey = it },
                isVisible = showGroqKey,
                onToggleVisibility = { showGroqKey = !showGroqKey },
                prefix = "gsk_",
                isValid = groqKey.startsWith("gsk_")
            )
            if (groqKey.isEmpty() || !groqKey.startsWith("gsk_")) {
                Text(
                    text = "Add a free Groq key for 10x faster transcription. Get one at groq.com",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            ApiKeyField(
                label = "OpenAI API Key",
                value = openAiKey,
                onValueChange = { openAiKey = it },
                isVisible = showOpenAiKey,
                onToggleVisibility = { showOpenAiKey = !showOpenAiKey },
                prefix = "sk-",
                isValid = openAiKey.startsWith("sk-") && !openAiKey.startsWith("sk-or-") && !openAiKey.startsWith("sk-ant-")
            )
            Spacer(modifier = Modifier.height(16.dp))
            ApiKeyField(
                label = "Anthropic API Key",
                value = anthropicKey,
                onValueChange = { anthropicKey = it },
                isVisible = showAnthropicKey,
                onToggleVisibility = { showAnthropicKey = !showAnthropicKey },
                prefix = "sk-ant-",
                isValid = anthropicKey.startsWith("sk-ant-")
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Model Tier",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TierButton(
                text = "Cheap",
                selected = modelTier == ModelTier.CHEAP,
                onClick = { modelTier = ModelTier.CHEAP },
                modifier = Modifier.weight(1f)
            )
            TierButton(
                text = "Best",
                selected = modelTier == ModelTier.BEST,
                onClick = { modelTier = ModelTier.BEST },
                modifier = Modifier.weight(1f)
            )
            TierButton(
                text = "Custom",
                selected = modelTier == ModelTier.CUSTOM,
                onClick = { modelTier = ModelTier.CUSTOM },
                modifier = Modifier.weight(1f)
            )
        }

        ModelLabels(apiMode, modelTier, apiRouter, customSttModel, customRefinementModel,
            onSttChange = { customSttModel = it },
            onRefinementChange = { customRefinementModel = it })

        Spacer(modifier = Modifier.height(24.dp))

        CostEstimate(modelTier)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    isTestingApi = true

                    try {
                        // Save first
                        prefs.saveApiMode(apiMode)
                        prefs.saveModelTier(modelTier)
                        if (apiMode == ApiMode.OPENROUTER) {
                            prefs.saveOpenRouterKey(openRouterKey)
                            if (groqKey.isNotEmpty() && groqKey.startsWith("gsk_")) {
                                prefs.saveGroqKey(groqKey)
                            } else if (groqKey.isEmpty()) {
                                prefs.clearGroqKey()
                            }
                        } else {
                            prefs.saveOpenAiKey(openAiKey)
                            prefs.saveAnthropicKey(anthropicKey)
                        }
                        if (modelTier == ModelTier.CUSTOM) {
                            prefs.saveCustomSttModel(customSttModel)
                            prefs.saveCustomRefinementModel(customRefinementModel)
                        }

                        // Simulate/Perform test
                        val success = performApiTest(context, apiRouter, apiMode, modelTier)
                        if (success) {
                            snackbarHostState.showSnackbar("API Test Successful!")
                        } else {
                            snackbarHostState.showSnackbar("API Test Failed. Please check your keys.")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message ?: "Unknown error"}")
                    } finally {
                        isTestingApi = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isTestingApi && isKeySetupValid(apiMode, openRouterKey, openAiKey, anthropicKey)
        ) {
            if (isTestingApi) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Save & Test API", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        }

    }

    if (showSwitchModeDialog != null) {
        AlertDialog(
            onDismissRequest = { showSwitchModeDialog = null },
            title = { Text("Switch Mode?") },
            text = { Text("Switching modes will clear your current API keys. Do you want to continue?") },
            confirmButton = {
                TextButton(onClick = {
                    val targetMode = showSwitchModeDialog!!
                    if (targetMode == ApiMode.OPENROUTER) {
                        prefs.clearOpenAiKey()
                        prefs.clearAnthropicKey()
                        openAiKey = ""
                        anthropicKey = ""
                    } else {
                        prefs.clearOpenRouterKey()
                        prefs.clearGroqKey()
                        openRouterKey = ""
                        groqKey = ""
                    }
                    apiMode = targetMode
                    prefs.saveApiMode(targetMode)
                    showSwitchModeDialog = null
                }) {
                    Text("Clear & Switch", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchModeDialog = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    prefix: String,
    isValid: Boolean
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (isValid && value.isNotEmpty()) Color.Green else MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (isVisible) VisualTransformation.None else MaskedKeyTransformation(),
            trailingIcon = {
                IconButton(onClick = onToggleVisibility) {
                    Icon(
                        imageVector = if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            placeholder = { Text(prefix + "...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
            singleLine = true
        )
    }
}

@Composable
fun TierButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

data class ModelOption(val id: String, val displayName: String, val detail: String)

fun getSttModelOptions(apiMode: ApiMode, hasGroqKey: Boolean): List<ModelOption> {
    // Latest models only, sorted by cost (cheapest first)
    return when {
        apiMode == ApiMode.OPENROUTER && hasGroqKey -> listOf(
            ModelOption("distil-whisper-large-v3-en", "Distil Whisper EN", "\$0.02/hr \u00b7 English only"),
            ModelOption("whisper-large-v3-turbo", "Whisper v3 Turbo", "\$0.04/hr \u00b7 ~10% WER \u00b7 Multilingual"),
            ModelOption("whisper-large-v3", "Whisper v3", "\$0.11/hr \u00b7 ~8% WER \u00b7 Best accuracy")
        )
        apiMode == ApiMode.OPENROUTER -> listOf(
            ModelOption("google/gemini-2.5-flash", "Gemini 2.5 Flash", "~\$0.15/M \u00b7 Fast"),
            ModelOption("google/gemini-2.5-pro", "Gemini 2.5 Pro", "~\$1.25/M \u00b7 Great accuracy"),
            ModelOption("openai/gpt-4o-audio-preview", "GPT-4o Audio", "~\$2.50/M \u00b7 Best accuracy")
        )
        apiMode == ApiMode.SEPARATE -> listOf(
            ModelOption("gpt-4o-mini-transcribe", "GPT-4o Mini Transcribe", "~\$0.36/hr \u00b7 ~4% WER"),
            ModelOption("gpt-4o-transcribe", "GPT-4o Transcribe", "~\$0.36/hr \u00b7 2.46% WER \u00b7 Best")
        )
        else -> emptyList()
    }
}

fun getRefinementModelOptions(apiMode: ApiMode): List<ModelOption> {
    // Latest models only, sorted by cost (cheapest first)
    return when (apiMode) {
        ApiMode.OPENROUTER -> listOf(
            ModelOption("anthropic/claude-haiku-4-5", "Claude 4.5 Haiku", "\$1.00/M in \u00b7 Fast"),
            ModelOption("anthropic/claude-sonnet-4-5", "Claude 4.5 Sonnet", "\$3.00/M in \u00b7 Best quality")
        )
        ApiMode.SEPARATE -> listOf(
            ModelOption("claude-haiku-4-5-20251001", "Claude 4.5 Haiku", "\$1.00/M in \u00b7 Fast"),
            ModelOption("claude-sonnet-4-5-20250929", "Claude 4.5 Sonnet", "\$3.00/M in \u00b7 Best quality")
        )
        ApiMode.NO_KEYS -> emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDropdown(
    options: List<ModelOption>,
    selectedValue: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = options.find { it.id == selectedValue }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption?.displayName ?: if (selectedValue.isEmpty()) "Select model" else selectedValue,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                option.displayName,
                                fontWeight = if (option.id == selectedValue) FontWeight.Bold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                option.detail,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    onClick = {
                        onSelected(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ModelLabels(
    apiMode: ApiMode,
    modelTier: ModelTier,
    apiRouter: ApiRouterManager,
    customStt: String,
    customRefinement: String,
    onSttChange: (String) -> Unit,
    onRefinementChange: (String) -> Unit
) {
    val hasGroq = apiRouter.hasGroqKey()
    val sttModelLabel = when (modelTier) {
        ModelTier.CUSTOM -> customStt
        ModelTier.CHEAP -> if (apiMode == ApiMode.OPENROUTER) {
            if (hasGroq) "whisper-large-v3-turbo (Groq)" else "google/gemini-2.5-flash"
        } else "gpt-4o-mini-transcribe"
        ModelTier.BEST -> if (apiMode == ApiMode.OPENROUTER) {
            if (hasGroq) "whisper-large-v3-turbo (Groq)" else "openai/gpt-4o-audio-preview"
        } else "gpt-4o-transcribe"
    }

    val refinementModelLabel = when (modelTier) {
        ModelTier.CUSTOM -> customRefinement
        ModelTier.CHEAP -> if (apiMode == ApiMode.OPENROUTER) "anthropic/claude-haiku-4-5" else "claude-haiku-4-5"
        ModelTier.BEST -> if (apiMode == ApiMode.OPENROUTER) "anthropic/claude-sonnet-4-5" else "claude-sonnet-4-5"
    }

    val sttOptions = getSttModelOptions(apiMode, hasGroq)
    val refinementOptions = getRefinementModelOptions(apiMode)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Transcription Model", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
        if (modelTier == ModelTier.CUSTOM) {
            ModelDropdown(
                options = sttOptions,
                selectedValue = customStt,
                onSelected = onSttChange
            )
        } else {
            Text(sttModelLabel, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Refinement Model", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
        if (modelTier == ModelTier.CUSTOM) {
            ModelDropdown(
                options = refinementOptions,
                selectedValue = customRefinement,
                onSelected = onRefinementChange
            )
        } else {
            Text(refinementModelLabel, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun CostEstimate(tier: ModelTier) {
    val cost = when (tier) {
        ModelTier.CHEAP -> "$4.80"
        ModelTier.BEST -> "$8.50"
        ModelTier.CUSTOM -> "Variable"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Est. ~$cost/mo (50 rec/day, 30s avg)",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Based on current provider rates. Actual usage may vary.",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

private fun isKeySetupValid(mode: ApiMode, orKey: String, oaiKey: String, antKey: String): Boolean {
    return when (mode) {
        ApiMode.OPENROUTER -> orKey.startsWith("sk-or-")
        ApiMode.SEPARATE -> oaiKey.startsWith("sk-") && !oaiKey.startsWith("sk-or-") && !oaiKey.startsWith("sk-ant-") && antKey.startsWith("sk-ant-")
        ApiMode.NO_KEYS -> false
    }
}

class MaskedKeyTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.length <= 12) {
            return TransformedText(
                AnnotatedString("*".repeat(originalText.length)),
                OffsetMapping.Identity
            )
        }
        val visibleStart = originalText.take(8)
        val visibleEnd = originalText.takeLast(4)
        val maskedPart = "*".repeat(originalText.length - 12)
        val transformedText = visibleStart + maskedPart + visibleEnd

        return TransformedText(
            AnnotatedString(transformedText),
            OffsetMapping.Identity
        )
    }
}

private suspend fun performApiTest(context: android.content.Context, apiRouter: ApiRouterManager, mode: ApiMode, tier: ModelTier): Boolean {
    if (!NetworkUtils.isOnline(context)) {
        throw IllegalStateException("No internet connection")
    }
    val client = apiRouter.getRefinementClient() ?: return false
    val model = apiRouter.getRefinementModel(tier)

    return client.validateKey(model)
}

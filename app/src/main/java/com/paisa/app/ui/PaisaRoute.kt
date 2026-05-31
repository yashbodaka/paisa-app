package com.paisa.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paisa.app.PaisaAppWidgetProvider
import com.paisa.app.audio.AudioCapture
import com.paisa.app.audio.VoskTranscriber
import kotlinx.coroutines.launch

@Composable
fun PaisaRoute(
    editTransactionId: Long? = null,
    onEditTransactionHandled: () -> Unit = {},
    startVoiceInput: Boolean = false,
    onVoiceInputHandled: () -> Unit = {},
    viewModel: PaisaViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Lazy Vosk transcriber
    val transcriber = remember { VoskTranscriber(context) }

    DisposableEffect(Unit) { onDispose { transcriber.close() } }

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.showMessage("Microphone ready — tap the mic to speak")
        else viewModel.showMessage("Microphone permission required for voice input")
    }

    // Permission launcher for SMS
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) viewModel.showMessage("SMS Sync active — checking for bank messages")
        else viewModel.showMessage("SMS permission denied — auto-sync disabled")
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            smsPermissionLauncher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
        }
    }

    // Whether to stop the current recording loop
    var stopRecording by remember { mutableStateOf(false) }

    val triggerVoiceInput: () -> Unit = {
        when {
            // ── Already listening → stop recording ──────────────────────
            state.isListening -> {
                stopRecording = true
                viewModel.setListening(false)
            }

            // ── Not listening → check permission then start ───────────
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            else -> {
                stopRecording = false
                viewModel.setListening(true)

                scope.launch {
                    try {
                        // 1. Record until user taps again (max 10 s)
                        val audio = AudioCapture.record(
                            maxSeconds = 10,
                            stopSignal = { stopRecording }
                        )

                        // 2. Show transcribing indicator
                        viewModel.setListening(false)
                        viewModel.setTranscribing(true)

                        // 3. Run Whisper offline
                        val text = transcriber.transcribe(audio)

                        // 4. Submit result
                        viewModel.onTranscribed(text)

                    } catch (e: Exception) {
                        viewModel.setListening(false)
                        viewModel.setTranscribing(false)
                        val friendly = when {
                            e.message?.contains("Missing Input") == true ||
                            e.message?.contains("ORT_RUNTIME") == true ->
                                "Voice model error — try reinstalling the app"
                            e.message?.contains("not found") == true ->
                                "Voice model not set up yet"
                            e.message?.contains("PERMISSION") == true ->
                                "Microphone permission denied"
                            else ->
                                "Voice input failed — please try again"
                        }
                        viewModel.showMessage(friendly)
                    }
                }
            }
        }
    }

    LaunchedEffect(startVoiceInput) {
        if (startVoiceInput) {
            onVoiceInputHandled()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                triggerVoiceInput()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(state.transactions, state.isLoading) {
        if (!state.isLoading) {
            PaisaAppWidgetProvider.updateAllWidgets(context)
        }
    }

    PaisaScreen(
        state = state,
        onDraftChange = viewModel::updateDraft,
        onSubmit = viewModel::submitDraft,
        onSuggestionClick = viewModel::submitText,
        onDelete = viewModel::delete,
        onUpdate = viewModel::updateTransaction,
        editTransactionId = editTransactionId,
        onEditTransactionHandled = onEditTransactionHandled,
        onVoiceClick = triggerVoiceInput
    )
}

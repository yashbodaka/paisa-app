package com.paisa.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.paisa.app.ui.PaisaRoute
import com.paisa.app.ui.theme.PaisaTheme

class MainActivity : ComponentActivity() {
    private var transactionIdToEdit by mutableStateOf<Long?>(null)
    private var startVoiceInput by mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transactionIdToEdit = intent.editTransactionId()
        startVoiceInput = intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PaisaTheme {
                PaisaRoute(
                    editTransactionId = transactionIdToEdit,
                    onEditTransactionHandled = { transactionIdToEdit = null },
                    startVoiceInput = startVoiceInput,
                    onVoiceInputHandled = { startVoiceInput = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        transactionIdToEdit = intent.editTransactionId()
        if (intent.getBooleanExtra(EXTRA_START_VOICE_INPUT, false)) {
            startVoiceInput = true
        }
    }

    private fun Intent.editTransactionId(): Long? {
        val id = getLongExtra(EXTRA_EDIT_TRANSACTION_ID, NO_TRANSACTION_ID)
        return id.takeIf { it != NO_TRANSACTION_ID }
    }

    companion object {
        const val ACTION_EDIT_TRANSACTION = "com.paisa.app.action.EDIT_TRANSACTION"
        const val EXTRA_EDIT_TRANSACTION_ID = "com.paisa.app.extra.EDIT_TRANSACTION_ID"
        const val EXTRA_START_VOICE_INPUT = "com.paisa.app.extra.START_VOICE_INPUT"
        private const val NO_TRANSACTION_ID = -1L
    }
}

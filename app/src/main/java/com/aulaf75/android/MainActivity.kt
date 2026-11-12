package com.aulaf75.android

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.aulaf75.android.ui.AppScreen
import com.aulaf75.android.ui.AulaTheme

/** AppCompatActivity so the in-app language picker also works below Android 13. */
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AulaTheme(vm.ui.themeMode) { AppScreen(vm.ui, vm) }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** When Android launched us because the keyboard was plugged in, permission is already granted. */
    private fun handleIntent(intent: Intent?) {
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED && !vm.ui.connected) vm.connect()
    }
}

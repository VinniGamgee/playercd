package com.moonplayer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moonplayer.app.data.preferences.ThemeMode
import com.moonplayer.app.ui.navigation.MoonNavHost
import com.moonplayer.app.ui.theme.MoonPlayerTheme
import com.moonplayer.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        setContent {
            val vm: MainViewModel = viewModel()
            val appSettings by vm.appSettings.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (appSettings.theme) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
                ThemeMode.SYSTEM -> systemDark
            }
            MoonPlayerTheme(
                darkTheme = dark,
                accentPreset = appSettings.accent,
                density = appSettings.density,
                cornerRadius = appSettings.cornerRadius,
                amoled = appSettings.theme == ThemeMode.AMOLED
            ) {
                MoonNavHost(vm)
            }
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.READ_MEDIA_AUDIO)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
    }
}

package com.ammu.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.ammu.player.ui.screens.AmmuMainScreen
import com.ammu.player.ui.screens.FullscreenPlayerSheet
import com.ammu.player.ui.theme.AmmuTheme
import com.ammu.player.ui.viewmodel.AmmuMainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AmmuMainViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Notification permission granted for Media3 controls
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val isAmoledMode by viewModel.isAmoledMode.collectAsState()
            var isFullscreenPlayerOpen by remember { mutableStateOf(false) }

            AmmuTheme(isAmoledMode = isAmoledMode) {
                // Android Back Gesture Handler
                BackHandler(enabled = isFullscreenPlayerOpen || viewModel.multiSelectMode.value) {
                    if (isFullscreenPlayerOpen) {
                        isFullscreenPlayerOpen = false
                    } else if (viewModel.multiSelectMode.value) {
                        viewModel.selectAllTracks() // Toggle off
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AmmuMainScreen(
                        viewModel = viewModel,
                        onOpenFullscreenPlayer = { isFullscreenPlayerOpen = true }
                    )

                    // Fullscreen Player Sheet with Slide Animation
                    AnimatedVisibility(
                        visible = isFullscreenPlayerOpen,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        FullscreenPlayerSheet(
                            viewModel = viewModel,
                            onDismiss = { isFullscreenPlayerOpen = false }
                        )
                    }
                }
            }
        }
    }
}

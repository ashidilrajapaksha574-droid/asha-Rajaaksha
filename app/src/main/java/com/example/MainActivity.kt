package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.LockState
import com.example.viewmodel.VideoViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: VideoViewModel = viewModel()
        val lockState by viewModel.lockState.collectAsState()

        if (lockState == LockState.UNLOCKED) {
          MainScreen(viewModel = viewModel)
        } else {
          LockScreen(viewModel = viewModel)
        }
      }
    }
  }
}

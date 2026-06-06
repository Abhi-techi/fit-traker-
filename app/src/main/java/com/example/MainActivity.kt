package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.Repository
import com.example.ui.FitTrackApp
import com.example.ui.FitViewModel
import com.example.ui.FitViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val repository = Repository(applicationContext)
    val viewModel = ViewModelProvider(this, FitViewModelFactory(repository, applicationContext))[FitViewModel::class.java]

    setContent {
      MyApplicationTheme {
        FitTrackApp(viewModel)
      }
    }
  }
}

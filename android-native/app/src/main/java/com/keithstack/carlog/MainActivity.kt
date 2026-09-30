package com.keithstack.carlog

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.keithstack.carlog.ui.CarLogApp
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.theme.CarLogTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CarLogViewModel by viewModels()

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.startLocationTracking()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarLogTheme {
                CarLogApp(viewModel = viewModel)
            }
        }
        requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

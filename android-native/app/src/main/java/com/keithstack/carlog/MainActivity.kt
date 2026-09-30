package com.keithstack.carlog

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.keithstack.carlog.ui.CarLogApp
import com.keithstack.carlog.ui.CarLogViewModel
import com.keithstack.carlog.ui.theme.CarLogTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CarLogViewModel by viewModels()

    private val googleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(this, options)
    }

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.startLocationTracking()
    }

    private val googleSignIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val idToken = try {
            task.getResult(com.google.android.gms.common.api.ApiException::class.java)?.idToken
        } catch (e: com.google.android.gms.common.api.ApiException) {
            null
        }
        viewModel.handleGoogleSignInToken(idToken)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarLogTheme {
                CarLogApp(
                    viewModel = viewModel,
                    onSignInWithGoogle = { googleSignIn.launch(googleSignInClient.signInIntent) },
                )
            }
        }
        requestPermissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
}

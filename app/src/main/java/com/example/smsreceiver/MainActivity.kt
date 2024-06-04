package com.example.smsreceiver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.smsreceiver.presentation.SMSScreen
import com.example.smsreceiver.presentation.SMSViewModel
import com.example.smsreceiver.ui.theme.SMSReceiverTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel by viewModels<SMSViewModel>()
        var isDeniedPermanently by mutableStateOf(false)

        setContent {
            SMSReceiverTheme {

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            Intent(
                                this@MainActivity.applicationContext,
                                SMSService::class.java
                            ).also {
                                startService(it)
                            }
                        } else {
                            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                    this@MainActivity,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            ) {
                                isDeniedPermanently = true
                            }
                        }
                    }

                    var showRationalDialog by remember {
                        mutableStateOf(false)
                    }

                    val scope = rememberCoroutineScope()

                    if (showRationalDialog) {
                        AlertDialog(
                            title = {
                                Text(text = "Permission Required")
                            },
                            text = {
                                Text(text = "We need the permission to receive the SMS messages.")
                            },
                            onDismissRequest = {
                                showRationalDialog = false
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    scope.launch {
                                        permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                                        showRationalDialog = false
                                    }
                                }) {
                                    Text(text = "Ok")
                                }
                            }
                        )
                    }
                    // Request the permission
                    OnResumeListener {
                        if (isDeniedPermanently.not()) {
                            if (ActivityCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.RECEIVE_SMS
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                Intent(
                                    this@MainActivity.applicationContext,
                                    SMSService::class.java
                                ).also {
                                    startService(it)
                                }
                            } else {
                                if (ActivityCompat.shouldShowRequestPermissionRationale(
                                        this@MainActivity,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                ) {
                                    showRationalDialog = true
                                } else {
                                    permissionLauncher.launch(
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                }
                            }
                        }
                    }

                    if (isDeniedPermanently) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You should enable the Receive SMS permission from the settings",
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        SMSScreen(viewModel)
                    }
                }
            }
        }
    }
}


@Composable
fun OnResumeListener(
    onResume: () -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(key1 = lifecycleOwner) {
        val lifecycelObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycelObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycelObserver)
        }
    }
}
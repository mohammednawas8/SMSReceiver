package com.example.smsreceiver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.example.smsreceiver.model.SMS
import com.example.smsreceiver.presentation.SMSScreen
import com.example.smsreceiver.presentation.SMSViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<SMSViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
        )

        setContent {

            val permissionsLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) {
                    viewModel.permissionsGranted = it.values.all { it }
                    if (viewModel.permissionsGranted) {
                        fetchSMSMessagesFromSpecificDate()
                        fetchMMS()
                    }
                }

            LaunchedEffect(true) {
                if (
                    ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECEIVE_SMS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    viewModel.permissionsGranted = true
                    fetchSMSMessagesFromSpecificDate()
                    fetchMMS()
                } else {
                    permissionsLauncher.launch(permissions)
                    viewModel.permissionsGranted = false
                }
            }

            if (viewModel.permissionsGranted) {
                SMSScreen(viewModel = viewModel)
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        permissionsLauncher.launch(permissions)
                    }) {
                        Text(text = "Grant Permission")
                    }
                }
            }
        }

    }

    private val smsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {

            fetchSMSMessagesFromSpecificDate()
            fetchMMS()
        }
    }

    override fun onResume() {
        super.onResume()

        fetchSMSMessagesFromSpecificDate()
        fetchMMS()

        contentResolver.registerContentObserver(
            Uri.parse("content://sms"), true, smsContentObserver
        )

        contentResolver.registerContentObserver(
            Uri.parse("content://mms"), true, smsContentObserver
        )
    }

    private fun fetchSMSMessagesFromSpecificDate() {
        lifecycleScope.launch {
            if (viewModel.permissionsGranted) {
                getAppInstallationDate(this@MainActivity)?.let { startDateString ->
                    var startDateMillis: Long = 0

                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
                        val date: Date? = dateFormat.parse(startDateString)
                        if (date != null) {
                            startDateMillis = date.time
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val smsUri: Uri = Uri.parse("content://sms/inbox")
                    val projection = arrayOf("_id", "address", "date", "body")
                    val selection = "date >= ?"
                    val selectionArgs = arrayOf(startDateMillis.toString())
                    val sortOrder = "date ASC"

                    val cursor: Cursor? =
                        contentResolver.query(
                            smsUri,
                            projection,
                            selection,
                            selectionArgs,
                            sortOrder
                        )

                    val smsList = mutableListOf<SMS>()
                    cursor?.use {
                        while (it.moveToNext()) {
                            val id = it.getString(it.getColumnIndexOrThrow("_id"))
                            val address = it.getString(it.getColumnIndexOrThrow("address"))
                            val date = it.getLong(it.getColumnIndexOrThrow("date"))
                            val body = it.getString(it.getColumnIndexOrThrow("body"))
                            smsList.add(
                                SMS(
                                    number = address,
                                    body = body,
                                    date = date,
                                    isRCSMessage = false
                                )
                            )
                        }
                    }
                    viewModel.addSMSList(smsList = smsList)
                }
            }
        }
    }

    @SuppressLint("Range")
    fun fetchMMS() {
        if (viewModel.permissionsGranted) {
            lifecycleScope.launch {
                getAppInstallationDate(this@MainActivity)?.let { installationDate ->
                    val uri = Uri.parse("content://mms/inbox")
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                    val installationDateInMillis =
                        formatter.parse(installationDate)?.time ?: System.currentTimeMillis()

                    val projection = arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE_SENT, "date")

                    val smsList = mutableListOf<SMS>()

                    contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            // Retrieve MMS ID
                            val mmsId = cursor.getInt(cursor.getColumnIndex(Telephony.Mms._ID))

                            // Get message body
                            val messageBody = getMessageBody(this@MainActivity, mmsId)

                            // Get number
                            val number = getNumber(this@MainActivity, mmsId)

                            println("test12345: $number $messageBody")

                            val dateInMillis =
                                cursor.getLong(cursor.getColumnIndexOrThrow("date")) * 1000L

                            if (dateInMillis >= installationDateInMillis) {
                                smsList.add(
                                    SMS(
                                        number = number,
                                        body = messageBody,
                                        date = dateInMillis,
                                        isRCSMessage = true
                                    )
                                )
                            }
                        }
                        viewModel.addSMSList(smsList)
                    }
                }

            }
        }
        // URI for MMS messages
    }

    @SuppressLint("Range")
    private fun getMessageBody(context: Context, mmsId: Int): String {
        // URI for retrieving message body
        val uri = Uri.parse("content://mms/part")

        // Columns to retrieve
        val projection = arrayOf("text")

        // Selection criteria
        val selection = "mid=$mmsId"

        // Query the MMS part content provider
        return context.contentResolver.query(uri, projection, selection, null, null)
            ?.use { cursor ->
                val stringBuilder = StringBuilder()
                while (cursor.moveToNext()) {
                    // Retrieve message body
                    stringBuilder.append(cursor.getString(cursor.getColumnIndex("text")))
                }
                stringBuilder.toString()
            } ?: ""
    }


    @SuppressLint("Range")
    private fun getNumber(context: Context, mmsId: Int): String {
        // URI for retrieving address
        val uri = Uri.parse("content://mms/$mmsId/addr")

        // Columns to retrieve
        val projection = arrayOf("address", "type")

        // Query the MMS address content provider
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                // Retrieve number
                val address = cursor.getString(cursor.getColumnIndex("address"))
                val type = cursor.getInt(cursor.getColumnIndex("type"))
                if (type == 137 && !address.isNullOrEmpty()) {
                    return address
                }
            }
            "Unknown number"
        } ?: "Unknown number"
    }
}

fun getAppInstallationDate(context: Context): String? {
    return try {
        val packageManager: PackageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        val installTimeInMillis = packageInfo.firstInstallTime
        val installDate = Date(installTimeInMillis)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateFormat.format(installDate)
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }
}

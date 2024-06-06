package com.example.smsreceiver.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.smsreceiver.model.SMS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class SMSViewModel(
    application: Application
) : AndroidViewModel(application) {

    var permissionsGranted by mutableStateOf(false)

    val smsList = MutableStateFlow<List<SMS>>(listOf())

    fun addSMSList(smsList: List<SMS>) {
        this.smsList.update {
            val currentList = it.toMutableList()
            val newList = (currentList + smsList).sortedByDescending { it.date }.distinct()
            newList
        }
    }
}
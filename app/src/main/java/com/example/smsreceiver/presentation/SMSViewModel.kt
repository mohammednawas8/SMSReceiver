package com.example.smsreceiver.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsreceiver.db.SMSDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SMSViewModel(
    application: Application
): AndroidViewModel(application) {

    private val dao = SMSDatabase.create(application)

    val smsList = dao
        .getAllSMS()
        .map { it.reversed() }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

}
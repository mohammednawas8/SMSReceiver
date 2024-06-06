package com.example.smsreceiver.model

data class SMS(
    val number: String,
    val body: String,
    val date: Long,
    val isRCSMessage: Boolean
)
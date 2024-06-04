package com.example.smsreceiver.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class SMS(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val number: String,
    val body: String
)
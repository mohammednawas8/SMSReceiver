package com.example.smsreceiver.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import com.example.smsreceiver.model.SMS
import kotlinx.coroutines.flow.Flow

@Dao
interface SMSDao {

    @Insert
    suspend fun insertSMS(sms: SMS)

    @Query("SELECT * FROM SMS")
    fun getAllSMS(): Flow<List<SMS>>

}
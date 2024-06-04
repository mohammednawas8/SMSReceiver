package com.example.smsreceiver.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.smsreceiver.model.SMS

@Database(entities = [SMS::class], version = 1)
abstract class SMSDatabase : RoomDatabase() {

    abstract fun smsDao(): SMSDao

    companion object {
        private var dao: SMSDao? = null
        fun create(context: Context): SMSDao {
            if (dao == null) {
                dao = Room.databaseBuilder(
                    context,
                    SMSDatabase::class.java,
                    "sms_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .smsDao()
            }
            return dao!!
        }
    }

}
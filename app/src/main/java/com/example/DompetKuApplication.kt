package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.FinanceRepository

class DompetKuApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)
        repository = FinanceRepository(database.financeDao())
    }
}

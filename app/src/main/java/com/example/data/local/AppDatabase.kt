package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.dao.FinanceDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsAllocationEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class,
        TransactionEntity::class,
        SavingsGoalEntity::class,
        SavingsAllocationEntity::class,
        CategoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dompetku_database.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}

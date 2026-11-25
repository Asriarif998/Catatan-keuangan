package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val targetAmount: Long,
    val currentAmount: Long = 0L,
    val deadlineMillis: Long? = null,
    val linkedWalletId: Long? = null,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

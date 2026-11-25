package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_allocations",
    indices = [Index(value = ["goalId"]), Index(value = ["walletId"])],
)
data class SavingsAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val goalId: Long,
    val walletId: Long,
    /** Positive = allocated from wallet to goal; Negative = withdrawn from goal back to wallet */
    val amountDelta: Long,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

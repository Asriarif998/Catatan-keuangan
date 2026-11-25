package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["walletId"]),
        Index(value = ["targetWalletId"]),
        Index(value = ["type"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val type: TransactionType,
    val amount: Long,
    val walletId: Long,
    val targetWalletId: Long? = null,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

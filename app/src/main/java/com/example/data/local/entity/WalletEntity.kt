package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.WalletType

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val type: WalletType,
    val initialBalance: Long,
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

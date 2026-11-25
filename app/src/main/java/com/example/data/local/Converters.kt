package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletType

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    @TypeConverter
    fun fromWalletType(value: WalletType): String = value.name

    @TypeConverter
    fun toWalletType(value: String): WalletType =
        runCatching { WalletType.valueOf(value) }.getOrDefault(WalletType.CASH)
}

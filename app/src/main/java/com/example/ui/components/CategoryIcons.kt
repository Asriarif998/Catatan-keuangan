package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.domain.model.TransactionType
import com.example.domain.model.WalletType
import com.example.ui.theme.WalletSwatches

object CategoryIcons {
    fun iconForCategory(category: String, type: TransactionType): ImageVector {
        if (type == TransactionType.TRANSFER) return Icons.Rounded.SwapHoriz
        val key = category.lowercase()
        return when {
            key.contains("makan") || key.contains("minum") || key.contains("food") -> Icons.Rounded.Restaurant
            key.contains("transport") || key.contains("bensin") || key.contains("ojek") -> Icons.Rounded.DirectionsCar
            key.contains("belanja") || key.contains("shop") -> Icons.Rounded.ShoppingBag
            key.contains("tagihan") || key.contains("listrik") || key.contains("air") || key.contains("utilitas") -> Icons.Rounded.ReceiptLong
            key.contains("hiburan") || key.contains("nonton") -> Icons.Rounded.Movie
            key.contains("kesehatan") || key.contains("obat") || key.contains("dokter") -> Icons.Rounded.LocalHospital
            key.contains("pendidikan") || key.contains("kursus") || key.contains("buku") -> Icons.Rounded.School
            key.contains("keluarga") -> Icons.Rounded.FamilyRestroom
            key.contains("gaji") -> Icons.Rounded.Payments
            key.contains("bonus") || key.contains("hadiah") -> Icons.Rounded.CardGiftcard
            key.contains("usaha") || key.contains("freelance") -> Icons.Rounded.Work
            key.contains("invest") -> Icons.Rounded.TrendingUp
            key.contains("donasi") -> Icons.Rounded.Favorite
            else -> Icons.Rounded.Category
        }
    }

    fun iconForWalletType(type: WalletType): ImageVector {
        return when (type) {
            WalletType.CASH -> Icons.Rounded.AccountBalanceWallet
            WalletType.BANK -> Icons.Rounded.AccountBalance
            WalletType.EWALLET -> Icons.Rounded.Smartphone
            WalletType.SAVINGS -> Icons.Rounded.Savings
        }
    }

    fun walletColor(colorIndex: Int): Color {
        return WalletSwatches[colorIndex.mod(WalletSwatches.size)]
    }
}

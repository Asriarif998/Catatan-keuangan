package com.example.util

import com.example.domain.model.TransactionItem
import com.example.domain.model.TransactionType

data class ParsedCsvTransaction(
    val timestamp: Long,
    val type: TransactionType,
    val amount: Long,
    val walletName: String,
    val targetWalletName: String?,
    val category: String,
    val note: String,
)

data class CsvParseResult(
    val items: List<ParsedCsvTransaction>,
    val skippedRows: Int,
)

object CsvManager {
    private const val HEADER =
        "id,tanggal_iso,timestamp_ms,jenis,nominal,dompet_asal,dompet_tujuan,kategori,catatan"

    fun exportToCsv(transactions: List<TransactionItem>): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        for (tx in transactions.sortedByDescending { it.timestamp }) {
            val fields = listOf(
                tx.id.toString(),
                CycleDateHelper.formatIsoDateTime(tx.timestamp),
                tx.timestamp.toString(),
                tx.type.name,
                tx.amount.toString(),
                tx.walletName,
                tx.targetWalletName.orEmpty(),
                tx.category,
                tx.note,
            )
            sb.appendLine(fields.joinToString(",") { escapeCsvField(it) })
        }
        return sb.toString()
    }

    fun parseCsv(content: String): CsvParseResult {
        val lines = parseCsvLines(content.trimStart('\uFEFF'))
        if (lines.isEmpty()) return CsvParseResult(emptyList(), 0)

        val header = lines.first().map { it.trim().lowercase() }
        val hasHeader = header.contains("jenis") || header.contains("nominal") || header.contains("type")
        val dataRows = if (hasHeader) lines.drop(1) else lines

        val parsed = mutableListOf<ParsedCsvTransaction>()
        var skipped = 0

        for (cols in dataRows) {
            if (cols.all { it.isBlank() }) continue
            try {
                val item = if (hasHeader) {
                    parseByHeader(header, cols)
                } else {
                    parsePositional(cols)
                }
                if (item != null && item.amount > 0L) {
                    parsed.add(item)
                } else {
                    skipped++
                }
            } catch (_: Exception) {
                skipped++
            }
        }
        return CsvParseResult(parsed, skipped)
    }

    private fun parseByHeader(header: List<String>, cols: List<String>): ParsedCsvTransaction? {
        fun get(vararg names: String): String {
            for (name in names) {
                val idx = header.indexOf(name)
                if (idx in cols.indices) return cols[idx].trim()
            }
            return ""
        }

        val timestampMs = get("timestamp_ms", "timestamp").toLongOrNull()
        val isoDate = get("tanggal_iso", "tanggal", "date")
        val timestamp = timestampMs
            ?: CycleDateHelper.parseIsoDateTime(isoDate)
            ?: System.currentTimeMillis()

        val typeStr = get("jenis", "type").uppercase()
        val type = when (typeStr) {
            "INCOME", "PEMASUKAN" -> TransactionType.INCOME
            "EXPENSE", "PENGELUARAN" -> TransactionType.EXPENSE
            "TRANSFER" -> TransactionType.TRANSFER
            else -> return null
        }

        val amount = get("nominal", "amount").filter { it.isDigit() }.toLongOrNull() ?: return null
        val walletName = get("dompet_asal", "dompet", "wallet").ifBlank { "Tunai" }
        val targetWallet = get("dompet_tujuan", "target_wallet").ifBlank { null }
        val category = get("kategori", "category").ifBlank {
            if (type == TransactionType.TRANSFER) "Transfer Dompet" else "Lainnya"
        }
        val note = get("catatan", "note")

        return ParsedCsvTransaction(
            timestamp = timestamp,
            type = type,
            amount = amount,
            walletName = walletName,
            targetWalletName = if (type == TransactionType.TRANSFER) targetWallet else null,
            category = category,
            note = note,
        )
    }

    private fun parsePositional(cols: List<String>): ParsedCsvTransaction? {
        if (cols.size < 6) return null
        val timestamp = cols.getOrNull(2)?.toLongOrNull()
            ?: CycleDateHelper.parseIsoDateTime(cols.getOrNull(1).orEmpty())
            ?: System.currentTimeMillis()
        val type = runCatching { TransactionType.valueOf(cols[3].trim().uppercase()) }.getOrNull()
            ?: return null
        val amount = cols[4].filter { it.isDigit() }.toLongOrNull() ?: return null
        val wallet = cols[5].trim().ifBlank { "Tunai" }
        val target = cols.getOrNull(6)?.trim()?.ifBlank { null }
        val category = cols.getOrNull(7)?.trim().orEmpty().ifBlank { "Lainnya" }
        val note = cols.getOrNull(8)?.trim().orEmpty()
        return ParsedCsvTransaction(
            timestamp = timestamp,
            type = type,
            amount = amount,
            walletName = wallet,
            targetWalletName = if (type == TransactionType.TRANSFER) target else null,
            category = category,
            note = note,
        )
    }

    internal fun escapeCsvField(field: String): String {
        val needsQuotes = field.contains(',') || field.contains('"') || field.contains('\n') || field.contains('\r')
        return if (needsQuotes) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    internal fun parseCsvLines(input: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < input.length && input[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                    }
                    '\r' -> {
                        if (i + 1 < input.length && input[i + 1] == '\n') i++
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        result.add(currentRow.toList())
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        currentField.clear()
                        result.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            result.add(currentRow.toList())
        }
        return result
    }
}

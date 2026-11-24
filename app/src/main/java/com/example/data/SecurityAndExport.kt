package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object PinHasher {
    private const val APP_SALT = "tariq_almazwad_abu_sefein_sharjah_2026"

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$APP_SALT:${pin.trim()}".toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun matches(pin: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        return hashPin(pin) == storedHash || pin.trim() == storedHash
    }
}

object UrlValidator {
    fun isSafeHttpUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return false
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        return (scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()
    }
}

object CsvExporter {
    private fun sanitizeCell(raw: String): String {
        val cleaned = raw.replace("\r", " ").replace("\n", " ").trim()
        val safe = if (cleaned.isNotEmpty() && cleaned[0] in listOf('=', '+', '-', '@')) {
            "'$cleaned"
        } else {
            cleaned
        }
        val escaped = safe.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    fun buildLeaderboardCsv(entries: List<LeaderboardEntry>): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel renders Arabic text cleanly
        sb.append('\uFEFF')
        sb.appendLine("الترتيب,الاسم بالكامل,رقم الهاتف,النقاط المعتمدة,نقاط البونص اليدوية,نقاط قيد المراجعة,عدد المهام المعتمدة,الأهلية للجائزة")
        entries.forEach { item ->
            val prizeStatus = if (item.rank in 1..FastCalendar.TOP_PRIZE_RANKS && item.totalApprovedPoints > 0) {
                "مؤهل للجائزة"
            } else {
                "مشارك"
            }
            sb.appendLine(
                listOf(
                    item.rank.toString(),
                    sanitizeCell(item.user.fullName),
                    sanitizeCell(item.user.phone.ifBlank { "-" }),
                    item.totalApprovedPoints.toString(),
                    item.user.bonusPoints.toString(),
                    item.pendingPoints.toString(),
                    item.approvedTasksCount.toString(),
                    sanitizeCell(prizeStatus)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun exportAndShareCsv(context: Context, entries: List<LeaderboardEntry>): Result<String> {
        return runCatching {
            val csvContent = buildLeaderboardCsv(entries)
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "tariq_almazwad_report.csv")
            file.writeText(csvContent, StandardCharsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "تقرير نقاط تحدي طريق المزود - صوم الميلاد المجيد")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(intent, "تصدير تقرير النقاط (CSV / Excel)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            "تم إنشاء ملف التقرير (tariq_almazwad_report.csv) بنجاح"
        }
    }
}

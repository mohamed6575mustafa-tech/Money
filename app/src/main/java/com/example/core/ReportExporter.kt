package com.example.core

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.domain.BudgetTransaction
import com.example.domain.CategorySpendingSummary
import com.example.domain.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {
    fun exportCsvAndShare(
        context: Context,
        title: String,
        transactions: List<BudgetTransaction>,
        currencyCode: String
    ): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "masrouf_report_$stamp.csv")
        FileOutputStream(file).use { out ->
            // UTF-8 BOM for proper Arabic display in Microsoft Excel
            out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val sb = StringBuilder()
            sb.appendLine("تقرير:,$title")
            sb.appendLine("التاريخ,النوع,التصنيف,المبلغ (${currencyCode}),جهة الشراء / المصدر,المسجل,ملاحظات")
            transactions.forEach { tx ->
                val typeLabel = if (tx.type == TransactionType.EXPENSE) "مصروف" else "دخل"
                val dateLabel = MoneyUtils.formatDateAr(tx.dateEpochMillis).replace(",", " ")
                val amount = String.format(Locale.US, "%.2f", tx.amountPiasters / 100.0)
                val merchant = (tx.merchant ?: tx.incomeSource?.labelAr ?: "").replace(",", " ")
                val notes = (tx.notes ?: "").replace(",", " ").replace("\n", " ")
                sb.appendLine("$dateLabel,$typeLabel,${tx.categoryName},$amount,$merchant,${tx.addedByName},$notes")
            }
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        }
        shareFile(context, file, "text/csv", "مشاركة تقرير مصروف البيت (Excel/CSV)")
    }

    fun exportPdfAndShare(
        context: Context,
        title: String,
        periodLabel: String,
        totalIncomePiasters: Long,
        totalExpensePiasters: Long,
        categorySummaries: List<CategorySpendingSummary>,
        transactions: List<BudgetTransaction>,
        currencyCode: String
    ): Result<Unit> = runCatching {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "masrouf_report_$stamp.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val headerBg = Paint().apply { color = Color.parseColor("#064E3B") }
        canvas.drawRect(0f, 0f, 595f, 110f, headerBg)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4AF37")
            textSize = 14f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("مصروف البيت — التقرير المالي", 555f, 48f, titlePaint)
        canvas.drawText("$title ($periodLabel)", 555f, 80f, subtitlePaint)

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textSize = 13f
        }
        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#064E3B")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 138f
        val netPiasters = totalIncomePiasters - totalExpensePiasters
        val summaryLines = listOf(
            "إجمالي الدخل: ${MoneyUtils.formatCurrency(totalIncomePiasters, currencyCode)}",
            "إجمالي المصروفات: ${MoneyUtils.formatCurrency(totalExpensePiasters, currencyCode)}",
            "صافي التوفير: ${MoneyUtils.formatCurrency(netPiasters, currencyCode)}"
        )
        drawRtlText(canvas, "ملخص الأداء المالي", sectionPaint, 40f, y, 515)
        y += 28f
        summaryLines.forEach { line ->
            drawRtlText(canvas, line, bodyPaint, 40f, y, 515)
            y += 22f
        }

        y += 14f
        drawRtlText(canvas, "أعلى التصنيفات استهلاكاً", sectionPaint, 40f, y, 515)
        y += 26f
        categorySummaries.filter { it.spentPiasters > 0 }.sortedByDescending { it.spentPiasters }.take(8).forEach { item ->
            val line = "• ${item.category.name}: ${MoneyUtils.formatCurrency(item.spentPiasters, currencyCode)} (${item.sharePercent.toInt()}%)"
            drawRtlText(canvas, line, bodyPaint, 40f, y, 515)
            y += 20f
        }

        y += 14f
        drawRtlText(canvas, "أحدث العمليات (${transactions.size} عملية)", sectionPaint, 40f, y, 515)
        y += 26f
        transactions.take(15).forEach { tx ->
            if (y < 790f) {
                val typeAr = if (tx.type == TransactionType.EXPENSE) "مصروف" else "دخل"
                val line = "${MoneyUtils.formatShortDateAr(tx.dateEpochMillis)} | $typeAr | ${tx.categoryName} | ${MoneyUtils.formatCurrency(tx.amountPiasters, currencyCode)}"
                drawRtlText(canvas, line, bodyPaint, 40f, y, 515)
                y += 20f
            }
        }

        doc.finishPage(page)
        FileOutputStream(file).use { out -> doc.writeTo(out) }
        doc.close()

        shareFile(context, file, "application/pdf", "مشاركة تقرير مصروف البيت (PDF)")
    }

    private fun drawRtlText(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float, width: Int) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

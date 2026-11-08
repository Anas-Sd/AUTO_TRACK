package com.autotrack.app.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.autotrack.app.data.Category
import com.autotrack.app.data.TransactionItem
import com.autotrack.app.ui.components.ExportFormat
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object LedgerExporter {

    fun export(
        context: Context,
        items: List<TransactionItem>,
        categories: List<Category>,
        baseName: String,
        format: ExportFormat,
        vaultCode: String = ""
    ) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val todayDate = sdf.format(Date())

        val sanitizedBase = baseName.trim().replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
        val cleanBase = if (sanitizedBase.isBlank()) "AutoTrack_Ledger" else sanitizedBase
        val fullFileName = "${cleanBase}_${todayDate}.${format.extension}"

        try {
            when (format) {
                ExportFormat.CSV -> exportCsv(context, items, categories, fullFileName)
                ExportFormat.PDF -> exportPdf(context, items, categories, fullFileName, todayDate, vaultCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportCsv(
        context: Context,
        items: List<TransactionItem>,
        categories: List<Category>,
        fullFileName: String
    ) {
        val sb = StringBuilder()
        sb.append("ID,Date,Time,Type,Category,Amount(INR),PaymentMethod,Vendor,Note\n")

        items.forEach { item ->
            val catObj = categories.find { c -> c.id == item.categoryId }
            val catName = catObj?.name ?: "Uncategorized"
            val datePart = item.occurredAt.take(10)
            val timePart = if (item.occurredAt.length >= 19) item.occurredAt.substring(11, 19) else ""
            val cleanVendor = (item.receiverVendor ?: "").replace("\"", "\"\"")
            val cleanNote = (item.note ?: "").replace("\"", "\"\"")
            val cleanCat = catName.replace("\"", "\"\"")

            sb.append("\"${item.id}\",\"$datePart\",\"$timePart\",\"${item.type.uppercase(Locale.ROOT)}\",\"$cleanCat\",${item.amount},\"${item.sourceApp}\",\"$cleanVendor\",\"$cleanNote\"\n")
        }

        val bytes = sb.toString().toByteArray(Charsets.UTF_8)
        saveAndShareFile(context, fullFileName, bytes, "text/csv", items.size)
    }

    private fun exportPdf(
        context: Context,
        items: List<TransactionItem>,
        categories: List<Category>,
        fullFileName: String,
        todayDate: String,
        vaultCode: String
    ) {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val marginLeft = 36f
        val marginRight = 559f
        val printableWidth = marginRight - marginLeft // 523f

        // Paints
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // slate-900
            textSize = 17f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // slate-500
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val headerBgPaint = Paint().apply {
            color = Color.rgb(16, 185, 129) // emerald-500
        }

        val headerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        val rowEvenBgPaint = Paint().apply {
            color = Color.WHITE
        }

        val rowOddBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // slate-50
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // slate-200
            strokeWidth = 0.5f
        }

        val cellTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59) // slate-800
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val expenseAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(225, 29, 72) // rose-600
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val incomeAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(5, 150, 105) // emerald-600
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        // Columns definitions: Total = 523
        // Date(90), Type(36), Category(94), Note/Vendor(175), Mode(45), Amount(83)
        val colWidths = floatArrayOf(90f, 36f, 94f, 175f, 45f, 83f)
        val colHeaders = arrayOf("Date & Time", "Type", "Category", "Note / Vendor", "Mode", "Amount (INR)")

        fun drawTableHeader(canvas: android.graphics.Canvas, startY: Float) {
            canvas.drawRect(marginLeft, startY, marginRight, startY + 20f, headerBgPaint)
            var currentX = marginLeft
            colHeaders.forEachIndexed { i, title ->
                val w = colWidths[i]
                if (i == colHeaders.size - 1) {
                    val p = Paint(headerTextPaint).apply { textAlign = Paint.Align.RIGHT }
                    canvas.drawText(title, currentX + w - 6f, startY + 13.5f, p)
                } else {
                    canvas.drawText(title, currentX + 6f, startY + 13.5f, headerTextPaint)
                }
                currentX += w
            }
        }

        fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
            if (paint.measureText(text) <= maxWidth) return text
            var len = text.length
            while (len > 0 && paint.measureText(text.take(len) + "…") > maxWidth) {
                len--
            }
            return if (len > 0) text.take(len) + "…" else ""
        }

        val totalIncome = items.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = items.filter { it.type != "income" }.sumOf { it.amount }
        val net = totalIncome - totalExpense

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas

        // Page 1 Header
        var currentY = 46f
        canvas.drawText("AutoTrack Ledger Report", marginLeft, currentY, titlePaint)

        currentY += 16f
        val vaultInfo = if (vaultCode.isNotBlank()) " | Vault: $vaultCode" else ""
        canvas.drawText("Generated on: $todayDate (IST)$vaultInfo | Total Records: ${items.size}", marginLeft, currentY, subTitlePaint)

        currentY += 14f
        val statsStr = String.format(
            Locale.US,
            "Total Income: ₹%,.2f   •   Total Outcome: ₹%,.2f   •   Net: ₹%,.2f",
            totalIncome,
            totalExpense,
            net
        )
        canvas.drawText(statsStr, marginLeft, currentY, subTitlePaint)

        currentY += 18f
        drawTableHeader(canvas, currentY)
        currentY += 20f

        val rowHeight = 20f

        items.forEachIndexed { index, item ->
            // Check if page overflow
            if (currentY + rowHeight > pageHeight - 45f) {
                // Draw footer
                canvas.drawText("Page $pageNumber", pageWidth / 2f, pageHeight - 20f, footerPaint)
                pdfDocument.finishPage(currentPage)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas

                // Redraw table header on new page
                currentY = 40f
                drawTableHeader(canvas, currentY)
                currentY += 20f
            }

            // Draw row background
            val isEven = index % 2 == 0
            canvas.drawRect(marginLeft, currentY, marginRight, currentY + rowHeight, if (isEven) rowEvenBgPaint else rowOddBgPaint)
            canvas.drawLine(marginLeft, currentY + rowHeight, marginRight, currentY + rowHeight, borderPaint)

            val catObj = categories.find { c -> c.id == item.categoryId }
            val catName = catObj?.name ?: "Uncategorized"

            val datePart = item.occurredAt.take(10)
            val timePart = if (item.occurredAt.length >= 16) item.occurredAt.substring(11, 16) else ""
            val dateTimeStr = if (timePart.isNotBlank()) "$datePart $timePart" else datePart

            val isIncome = item.type == "income"
            val typeStr = if (isIncome) "INC" else "EXP"
            val noteVendorStr = item.note ?: item.receiverVendor ?: "-"
            val modeStr = item.sourceApp
            val amountStr = String.format(Locale.US, "%s₹%,.2f", if (isIncome) "+ " else "- ", item.amount)

            // Cell columns
            var colX = marginLeft

            // 0: Date
            canvas.drawText(dateTimeStr, colX + 6f, currentY + 13.5f, cellTextPaint)
            colX += colWidths[0]

            // 1: Type
            val typePaint = Paint(cellTextPaint).apply {
                color = if (isIncome) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(typeStr, colX + 6f, currentY + 13.5f, typePaint)
            colX += colWidths[1]

            // 2: Category
            val truncatedCat = truncateText(catName, colWidths[2] - 10f, cellTextPaint)
            canvas.drawText(truncatedCat, colX + 6f, currentY + 13.5f, cellTextPaint)
            colX += colWidths[2]

            // 3: Note / Vendor
            val truncatedNote = truncateText(noteVendorStr, colWidths[3] - 10f, cellTextPaint)
            canvas.drawText(truncatedNote, colX + 6f, currentY + 13.5f, cellTextPaint)
            colX += colWidths[3]

            // 4: Mode
            canvas.drawText(modeStr, colX + 6f, currentY + 13.5f, cellTextPaint)
            colX += colWidths[4]

            // 5: Amount (Right aligned)
            val amtPaint = if (isIncome) incomeAmountPaint else expenseAmountPaint
            canvas.drawText(amountStr, colX + colWidths[5] - 6f, currentY + 13.5f, amtPaint)

            currentY += rowHeight
        }

        // Draw final page footer
        canvas.drawText("Page $pageNumber", pageWidth / 2f, pageHeight - 20f, footerPaint)
        pdfDocument.finishPage(currentPage)

        val byteArrayOutputStream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(byteArrayOutputStream)
        pdfDocument.close()

        val bytes = byteArrayOutputStream.toByteArray()
        saveAndShareFile(context, fullFileName, bytes, "application/pdf", items.size)
    }

    private fun saveAndShareFile(
        context: Context,
        fullFileName: String,
        bytes: ByteArray,
        mimeType: String,
        itemCount: Int
    ) {
        var savedUri: Uri? = null

        // 1. Save to device Public Downloads folder
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val uri = resolver.insert(collectionUri, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        out.write(bytes)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, fullFileName)
                FileOutputStream(targetFile).use { out ->
                    out.write(bytes)
                }
                savedUri = Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Also write to cache directory for seamless FileProvider sharing
        var shareableUri: Uri? = null
        try {
            val cacheFile = File(context.cacheDir, fullFileName)
            FileOutputStream(cacheFile).use { out ->
                out.write(bytes)
            }
            shareableUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            shareableUri = savedUri
        }

        Toast.makeText(
            context,
            "Saved $itemCount transactions to Downloads/$fullFileName",
            Toast.LENGTH_LONG
        ).show()

        // 3. Open Android Share / View Sheet
        try {
            val targetUri = shareableUri ?: savedUri
            if (targetUri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, targetUri)
                    putExtra(Intent.EXTRA_SUBJECT, fullFileName)
                    putExtra(Intent.EXTRA_TEXT, "Exported $itemCount transactions from AutoTrack.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Open or Share $fullFileName")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

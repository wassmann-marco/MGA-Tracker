package org.ausbildungstracker.project

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

actual fun downloadJsonDatei(inhalt: String, dateiname: String) {
    val context = AppContext.app
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, dateiname)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { it.write(inhalt.toByteArray(Charsets.UTF_8)) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            File(dir, dateiname).writeText(inhalt, Charsets.UTF_8)
        }
        Toast.makeText(context, "Backup gespeichert: $dateiname", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

actual fun pickAndImportJsonDatei(onResult: (String) -> Unit) {
    ActivityHolder.importCallback = onResult
    ActivityHolder.importLauncher?.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
}

actual fun formatiereDatum(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy").format(Date(millis))

actual fun aktuellesDatumMillis(): Long = System.currentTimeMillis()

actual fun formatUE(wert: Double): String = String.format("%.1f", wert)

actual fun generierePdf(eintraege: List<TrainingEintrag>, profil: UserProfile) {
    val context = AppContext.app
    try {
        val document = PdfDocument()
        // A4 Querformat in Points (1 pt = 1/72 inch, A4 = 297x210mm)
        val pageWidth = 842
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val margin = 30f
        var y = margin

        // --- Farben ---
        val rot = Color.rgb(125, 11, 35)
        val hellGrau = Color.rgb(221, 221, 221)
        val dunkelGrau = Color.rgb(50, 50, 50)
        val schwarz = Color.BLACK

        // --- Titel ---
        val titelPaint = Paint().apply {
            color = rot
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText("Ausbildungsnachweis – MGA Niedersachsen", margin, y + 16f, titelPaint)
        y += 26f

        // --- Profil-Info ---
        val infoPaint = Paint().apply {
            color = dunkelGrau
            textSize = 10f
            isAntiAlias = true
        }
        canvas.drawText(
            "Name: ${profil.vorname} ${profil.name}   |   Ortsfeuerwehr: ${profil.ortsfeuerwehr}   |   Erstellt: ${formatiereDatum(System.currentTimeMillis())}",
            margin, y + 10f, infoPaint
        )
        y += 20f

        // --- Tabelle ---
        // Spaltenbreiten (Summe = pageWidth - 2*margin = 782)
        val cols = floatArrayOf(62f, 38f, 170f, 110f, 100f, 38f, 264f)
        val headers = arrayOf("Datum", "Nr.", "Modul", "Ausbilder", "Zeitraum", "UE", "Bemerkung")
        val rowHeight = 18f
        val cellPadding = 4f

        val headerPaint = Paint().apply {
            color = schwarz
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val cellPaint = Paint().apply {
            color = dunkelGrau
            textSize = 8f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(150, 150, 150)
            strokeWidth = 0.5f
        }
        val headerBgPaint = Paint().apply {
            color = hellGrau
        }

        fun drawRow(canvas: Canvas, rowY: Float, cells: Array<String>, paint: Paint, bg: Paint?) {
            var x = margin
            if (bg != null) {
                canvas.drawRect(x, rowY, margin + cols.sum(), rowY + rowHeight, bg)
            }
            for (i in cols.indices) {
                canvas.drawLine(x, rowY, x, rowY + rowHeight, linePaint)
                val text = cells[i]
                // Kürzen wenn Text zu lang
                val maxChars = (cols[i] / 5f).toInt()
                val displayText = if (text.length > maxChars) text.take(maxChars - 1) + "…" else text
                canvas.drawText(displayText, x + cellPadding, rowY + rowHeight - cellPadding, paint)
                x += cols[i]
            }
            canvas.drawLine(x, rowY, x, rowY + rowHeight, linePaint)
            canvas.drawLine(margin, rowY + rowHeight, margin + cols.sum(), rowY + rowHeight, linePaint)
        }

        // Obere Linie der Tabelle
        canvas.drawLine(margin, y, margin + cols.sum(), y, linePaint)
        drawRow(canvas, y, headers, headerPaint, headerBgPaint)
        y += rowHeight

        // Datenzeilen
        val sortiertEintraege = eintraege.sortedBy { parseDateToEpochDays(it.datum) }
        for (e in sortiertEintraege) {
            if (y + rowHeight > pageHeight - margin) break // Überlauf verhindern
            val modulName = alleModule.find { it.id == e.modulId }?.name ?: "Unbekannt"
            val ue = ((e.endeH * 60 + e.endeM) - (e.startH * 60 + e.startM)) / 45.0
            val zeit = "${e.startH.toString().padStart(2,'0')}:${e.startM.toString().padStart(2,'0')} - ${e.endeH.toString().padStart(2,'0')}:${e.endeM.toString().padStart(2,'0')}"
            drawRow(canvas, y, arrayOf(e.datum, e.modulId, modulName, e.ausbilder, zeit, formatUE(ue), e.bemerkung), cellPaint, null)
            y += rowHeight
        }

        // --- Fußzeile ---
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText(
            "Dieser Ausdruck stammt aus der MGA-Tracker App für Niedersachsen.",
            margin, pageHeight - 10f, footerPaint
        )

        document.finishPage(page)

        // --- Speichern in Downloads ---
        val dateiname = "Dienstnachweis_${profil.name}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, dateiname)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
            resolver.openOutputStream(uri)?.use { document.writeTo(it) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            FileOutputStream(File(dir, dateiname)).use { document.writeTo(it) }
        }

        document.close()
        Toast.makeText(context, "PDF gespeichert: $dateiname", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "PDF-Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

package org.ausbildungstracker.project

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import com.lowagie.text.*
import com.lowagie.text.pdf.*
import java.awt.Color
import java.io.FileOutputStream

// =========================================================
// 1. REPOSITORY IMPLEMENTIERUNG FÜR DEN DESKTOP
// =========================================================

class DesktopRepository : TrainingRepository {
    private val speicher = MutableStateFlow<List<TrainingEintrag>>(emptyList())

    // Speicherpfade im Benutzerverzeichnis
    private val archivDatei = File(System.getProperty("user.home"), ".mga_tracker_data.json")
    private val profilDatei = File(System.getProperty("user.home"), ".mga_tracker_profil.json")

    private val jsonKonfig = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override fun holeAlleEintraege(): Flow<List<TrainingEintrag>> = speicher

    // Lädt die Trainingsdaten beim App-Start
    override suspend fun laden(): List<TrainingEintrag> {
        return if (archivDatei.exists()) {
            try {
                val inhalt = archivDatei.readText()
                val liste = jsonKonfig.decodeFromString(ListSerializer(TrainingEintrag.serializer()), inhalt)
                speicher.value = liste
                liste
            } catch (e: Exception) { emptyList() }
        } else emptyList()
    }

    override suspend fun speichereEintrag(eintrag: TrainingEintrag) {
        val liste = speicher.value.toMutableList()
        val index = liste.indexOfFirst { it.id == eintrag.id }
        if (index != -1) {
            liste[index] = eintrag
        } else {
            liste.add(eintrag)
        }
        speicher.value = liste
        saveDataToFile(liste)
    }

    override suspend fun loescheEintrag(id: Long) {
        val neueListe = speicher.value.filter { it.id != id }
        speicher.value = neueListe
        saveDataToFile(neueListe)
    }

    private fun saveDataToFile(liste: List<TrainingEintrag>) {
        try {
            val text = jsonKonfig.encodeToString(ListSerializer(TrainingEintrag.serializer()), liste)
            archivDatei.writeText(text)
        } catch (e: Exception) { println("Fehler beim Speichern: ${e.message}") }
    }

    // --- NEU: PROFIL FUNKTIONEN ---

    override suspend fun speichereProfil(profil: UserProfile) {
        try {
            val text = jsonKonfig.encodeToString(UserProfile.serializer(), profil)
            profilDatei.writeText(text)
        } catch (e: Exception) {
            println("Profil konnte nicht gespeichert werden: ${e.message}")
        }
    }

    override suspend fun ladeProfil(): UserProfile? {
        return if (profilDatei.exists()) {
            try {
                val inhalt = profilDatei.readText()
                jsonKonfig.decodeFromString(UserProfile.serializer(), inhalt)
            } catch (e: Exception) {
                null
            }
        } else null
    }
}

// =========================================================
// 2. PLATTFORM-BRÜCKEN (ACTUALS) FÜR DEN DESKTOP
// =========================================================

actual fun provideRepository(): TrainingRepository = DesktopRepository()

actual fun formatiereDatum(millis: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy")
    return formatter.format(Date(millis))
}

actual fun aktuellesDatumMillis(): Long = System.currentTimeMillis()

// WICHTIG: Die Brücke für formatUE
actual fun formatUE(wert: Double): String = String.format("%.1f", wert)

actual fun downloadJsonDatei(inhalt: String, dateiname: String) {
    val dialog = FileDialog(Frame(), "Backup speichern", FileDialog.SAVE)
    dialog.file = dateiname
    dialog.isVisible = true
    if (dialog.directory != null && dialog.file != null) {
        try {
            File(dialog.directory, dialog.file).writeText(inhalt)
        } catch (e: Exception) {}
    }
}

actual fun pickAndImportJsonDatei(onResult: (String) -> Unit) {
    val dialog = FileDialog(Frame(), "Backup auswählen", FileDialog.LOAD)
    dialog.file = "*.json"
    dialog.isVisible = true
    if (dialog.directory != null && dialog.file != null) {
        try {
            val inhalt = File(dialog.directory, dialog.file).readText()
            onResult(inhalt)
        } catch (e: Exception) {}
    }
}
// PDF Erzeugung mit OpenPDF
actual fun generierePdf(eintraege: List<TrainingEintrag>, profil: UserProfile) {
    val dialog = FileDialog(Frame(), "PDF speichern", FileDialog.SAVE)
    dialog.file = "Dienstnachweis_${profil.name}.pdf"
    dialog.isVisible = true

    if (dialog.directory != null && dialog.file != null) {
        val pfad = File(dialog.directory, dialog.file)

        // 1. QUERFORMAT (Landscape) einstellen
        val document = Document(PageSize.A4.rotate())

        try {
            PdfWriter.getInstance(document, FileOutputStream(pfad))
            document.open()

            // Header & Titel
            val fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
            val title = Paragraph("Ausbildungsnachweis - MGA Niedersachsen", fontTitle)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            document.add(Paragraph(" "))

            // Profil-Infos
            document.add(Paragraph("Name: ${profil.vorname} ${profil.name} | Ortsfeuerwehr: ${profil.ortsfeuerwehr}"))
            document.add(Paragraph("Erstellt am: ${formatiereDatum(System.currentTimeMillis())}"))
            document.add(Paragraph(" "))

            // Tabelle mit 7 Spalten
            // Spalten: Datum, ID, Modulname, Ausbilder, Zeitraum, UE, Bemerkung
            val table = PdfPTable(7)
            table.widthPercentage = 100f
            // Breiten festlegen (Modul-ID ist jetzt klein auf 6f)
            table.setWidths(floatArrayOf(10f, 6f, 22f, 15f, 15f, 6f, 26f))

            // Header-Zeile
            val fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)
            listOf("Datum", "Nr.", "Modul-Name", "Ausbilder", "Zeitraum", "UE", "Bemerkung").forEach {
                val cell = PdfPCell(Phrase(it, fontHeader))
                cell.backgroundColor = Color.LIGHT_GRAY
                cell.setPadding(5f)
                table.addCell(cell)
            }

            // Daten-Zeilen
            val fontCell = FontFactory.getFont(FontFactory.HELVETICA, 9f)
            eintraege.sortedBy { parseDateToEpochDays(it.datum) }.forEach { e ->
                val modulName = alleModule.find { it.id == e.modulId }?.name ?: "Unbekannt"

                table.addCell(PdfPCell(Phrase(e.datum, fontCell)))
                table.addCell(PdfPCell(Phrase(e.modulId, fontCell))) // Kleines Feld für Nummer
                table.addCell(PdfPCell(Phrase(modulName, fontCell))) // Neues Feld für Modulname
                table.addCell(PdfPCell(Phrase(e.ausbilder, fontCell)))
                table.addCell(PdfPCell(Phrase("${e.startH.toString().padStart(2, '0')}:${e.startM.toString().padStart(2, '0')} - ${e.endeH.toString().padStart(2, '0')}:${e.endeM.toString().padStart(2, '0')}", fontCell)))
                val ue = ((e.endeH * 60 + e.endeM) - (e.startH * 60 + e.startM)) / 45.0
                table.addCell(PdfPCell(Phrase(formatUE(ue), fontCell)))
                table.addCell(PdfPCell(Phrase(e.bemerkung, fontCell))) // Bemerkung dabei
            }

            document.add(table)

            // FUSSZEILE
            val footer = Paragraph("\nDieser Ausdruck stammt aus der MGA-Tracker App für Niedersachsen.", FontFactory.getFont(FontFactory.HELVETICA, 8f, Font.ITALIC))
            footer.alignment = Element.ALIGN_RIGHT
            document.add(footer)

            document.close()
        } catch (e: Exception) { e.printStackTrace() }
    }
}
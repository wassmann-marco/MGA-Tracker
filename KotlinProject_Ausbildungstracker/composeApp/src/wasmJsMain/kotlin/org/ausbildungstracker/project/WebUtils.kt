package org.ausbildungstracker.project

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

// =========================================================
// EXTERNE JAVASCRIPT-BRÜCKEN (WasmJS konform)
// =========================================================

@JsFun("(millis) => { const d = new Date(millis); const tag = String(d.getDate()).padStart(2, '0'); const monat = String(d.getMonth() + 1).padStart(2, '0'); return tag + '.' + monat + '.' + d.getFullYear(); }")
external fun jsFormatiereDatum(millis: Double): String

@JsFun("() => Date.now()")
external fun jsAktuellesDatumMillis(): Double

@JsFun("(wert) => wert.toFixed(1)")
external fun jsFormatUE(wert: Double): String

@JsFun("(inhalt, dateiname) => { const blob = new Blob([inhalt], {type: 'application/json'}); const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = dateiname; link.click(); URL.revokeObjectURL(url); }")
external fun jsDownloadJsonDatei(inhalt: String, dateiname: String)

@JsFun("(onResult) => { const input = document.createElement('input'); input.type = 'file'; input.accept = '.json'; input.onchange = (e) => { const file = e.target.files[0]; if(file) { const reader = new FileReader(); reader.onload = (el) => { onResult(el.target.result); }; reader.readAsText(file); } }; input.click(); }")
external fun jsPickAndImportJsonDatei(onResult: (String) -> Unit)

@JsFun("(html) => { const printWindow = window.open('', '_blank'); if(printWindow) { printWindow.document.open(); printWindow.document.write(html); printWindow.document.close(); setTimeout(() => { printWindow.print(); }, 500); } }")
external fun jsPrintWindow(html: String)


// =========================================================
// REPOSITORY IMPLEMENTIERUNG FÜR DAS WEB (WasmJS)
// =========================================================

class WebRepository : TrainingRepository {
    private val key = "mga_tracker_data"
    private val profilKey = "mga_tracker_profil"
    private val jsonKonfig = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val speicher = MutableStateFlow<List<TrainingEintrag>>(emptyList())

    override fun holeAlleEintraege(): Flow<List<TrainingEintrag>> = speicher

    override suspend fun laden(): List<TrainingEintrag> {
        val json = localStorage.getItem(key) ?: return emptyList()
        return try {
            // Eindeutige Serializer-Zuweisung verhindert den Typen-Inferenz-Fehler
            val liste = jsonKonfig.decodeFromString(ListSerializer(TrainingEintrag.serializer()), json)
            speicher.value = liste
            liste
        } catch (e: Exception) { emptyList() }
    }

    override suspend fun speichereEintrag(eintrag: TrainingEintrag) {
        val liste = speicher.value.toMutableList()
        val index = liste.indexOfFirst { it.id == eintrag.id }
        if (index != -1) liste[index] = eintrag else liste.add(eintrag)
        speicher.value = liste
        localStorage.setItem(key, jsonKonfig.encodeToString(ListSerializer(TrainingEintrag.serializer()), liste))
    }

    override suspend fun loescheEintrag(id: Long) {
        val liste = speicher.value.filter { it.id != id }
        speicher.value = liste
        localStorage.setItem(key, jsonKonfig.encodeToString(ListSerializer(TrainingEintrag.serializer()), liste))
    }

    override suspend fun speichereProfil(profil: UserProfile) {
        val json = jsonKonfig.encodeToString(UserProfile.serializer(), profil)
        localStorage.setItem(profilKey, json)
    }

    override suspend fun ladeProfil(): UserProfile? {
        val json = localStorage.getItem(profilKey) ?: return null
        return try {
            jsonKonfig.decodeFromString(UserProfile.serializer(), json)
        } catch (e: Exception) { null }
    }
}


// =========================================================
// PLATTFORM-BRÜCKEN (ACTUALS) FÜR WEB
// =========================================================

actual fun provideRepository(): TrainingRepository = WebRepository()

actual fun formatiereDatum(millis: Long): String = jsFormatiereDatum(millis.toDouble())

actual fun aktuellesDatumMillis(): Long = jsAktuellesDatumMillis().toLong()

actual fun formatUE(wert: Double): String = jsFormatUE(wert)

actual fun downloadJsonDatei(inhalt: String, dateiname: String) = jsDownloadJsonDatei(inhalt, dateiname)

actual fun pickAndImportJsonDatei(onResult: (String) -> Unit) = jsPickAndImportJsonDatei(onResult)

actual fun generierePdf(eintraege: List<TrainingEintrag>, profil: UserProfile) {
    var rowsHtml = ""
    eintraege.sortedBy { parseDateToEpochDays(it.datum) }.forEach { e ->
        val modulName = alleModule.find { it.id == e.modulId }?.name ?: "Unbekannt"
        val ue = ((e.endeH * 60 + e.endeM) - (e.startH * 60 + e.startM)) / 45.0
        val zeit = "${e.startH.toString().padStart(2, '0')}:${e.startM.toString().padStart(2, '0')} - ${e.endeH.toString().padStart(2, '0')}:${e.endeM.toString().padStart(2, '0')}"
        rowsHtml += "<tr><td>${e.datum}</td><td>${e.modulId}</td><td>$modulName</td><td>${e.ausbilder}</td><td>$zeit</td><td>${formatUE(ue)}</td><td>${e.bemerkung}</td></tr>"
    }

    val html = """
        <html>
        <head>
            <style>
                @page { size: landscape; margin: 10mm; }
                body { font-family: sans-serif; font-size: 12px; padding: 10px; }
                table { width: 100%; border-collapse: collapse; margin-top: 15px; }
                th, td { border: 1px solid #444; padding: 6px; text-align: left; }
                th { background-color: #ddd; }
            </style>
        </head>
        <body>
            <h2 style="text-align:center;">Ausbildungsnachweis - MGA Niedersachsen</h2>
            <p><strong>Name:</strong> ${profil.vorname} ${profil.name} | <strong>Ortsfeuerwehr:</strong> ${profil.ortsfeuerwehr}</p>
            <table>
                <thead>
                    <tr><th>Datum</th><th>Nr.</th><th>Modul</th><th>Ausbilder</th><th>Zeit</th><th>UE</th><th>Bemerkung</th></tr>
                </thead>
                <tbody>$rowsHtml</tbody>
            </table>
            <p style="text-align:right;font-style:italic;margin-top:20px;">Dieser Ausdruck stammt aus der MGA-Tracker App.</p>
        </body>
        </html>
    """.trimIndent()

    jsPrintWindow(html)
}
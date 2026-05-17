package org.ausbildungstracker.project

import java.text.SimpleDateFormat
import java.util.Date

actual fun downloadJsonDatei(inhalt: String, dateiname: String) {
    // TODO: Android-Implementierung mit ActivityResultLauncher (benötigt Activity-Context)
    println("Export auf Android: $dateiname")
}

actual fun pickAndImportJsonDatei(onResult: (String) -> Unit) {
    // TODO: Android-Implementierung mit ActivityResultLauncher (benötigt Activity-Context)
    println("Import auf Android aufgerufen")
}

actual fun formatiereDatum(millis: Long): String =
    SimpleDateFormat("dd.MM.yyyy").format(Date(millis))

actual fun aktuellesDatumMillis(): Long = System.currentTimeMillis()

actual fun formatUE(wert: Double): String = String.format("%.1f", wert)

actual fun generierePdf(eintraege: List<TrainingEintrag>, profil: UserProfile) {
    // TODO: Android-Implementierung mit PrintManager oder PDF-Bibliothek
    println("PDF-Generierung auf Android noch nicht implementiert")
}
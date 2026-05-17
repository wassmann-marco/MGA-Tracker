package org.ausbildungstracker.project

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class TrainingEintrag(
    val id: Long,
    val modulId: String,
    val datum: String,
    val ausbilder: String,
    val startH: Int,
    val startM: Int,
    val endeH: Int,
    val endeM: Int,
    val bemerkung: String = ""
)

// WICHTIG: Das Profil muss auch serialisierbar sein für den Auto-Save!
@Serializable
data class UserProfile(
    val vorname: String = "",
    val name: String = "",
    val ortsfeuerwehr: String = "",
    val ausbildungsstart: String = ""
)

interface TrainingRepository {
    fun holeAlleEintraege(): Flow<List<TrainingEintrag>>
    suspend fun speichereEintrag(eintrag: TrainingEintrag)
    suspend fun loescheEintrag(id: Long)

    // NEU: Für den Auto-Save
    suspend fun laden(): List<TrainingEintrag>
    suspend fun speichereProfil(profil: UserProfile)
    suspend fun ladeProfil(): UserProfile?
}

expect fun provideRepository(): TrainingRepository
expect fun downloadJsonDatei(inhalt: String, dateiname: String)
expect fun pickAndImportJsonDatei(onResult: (String) -> Unit)
expect fun formatiereDatum(millis: Long): String
expect fun aktuellesDatumMillis(): Long
// In Repository.kt ganz unten hinzufügen:
expect fun formatUE(wert: Double): String

expect fun generierePdf(eintraege: List<TrainingEintrag>, profil: UserProfile)
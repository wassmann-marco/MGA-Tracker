package org.ausbildungstracker.project

import android.app.Application
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object AppContext {
    lateinit var app: Application
}

class AndroidRepository : TrainingRepository {
    private val prefs by lazy { AppContext.app.getSharedPreferences("mga_tracker", Context.MODE_PRIVATE) }
    private val jsonKonfig = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val speicher = MutableStateFlow<List<TrainingEintrag>>(emptyList())

    override fun holeAlleEintraege(): Flow<List<TrainingEintrag>> = speicher

    override suspend fun laden(): List<TrainingEintrag> {
        val json = prefs.getString("eintraege", null) ?: return emptyList()
        return try {
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
        prefs.edit().putString("eintraege", jsonKonfig.encodeToString(ListSerializer(TrainingEintrag.serializer()), liste)).apply()
    }

    override suspend fun loescheEintrag(id: Long) {
        val liste = speicher.value.filter { it.id != id }
        speicher.value = liste
        prefs.edit().putString("eintraege", jsonKonfig.encodeToString(ListSerializer(TrainingEintrag.serializer()), liste)).apply()
    }

    override suspend fun loescheAlleEintraege() {
        speicher.value = emptyList()
        prefs.edit().remove("eintraege").apply()
    }

    override suspend fun speichereProfil(profil: UserProfile) {
        prefs.edit().putString("profil", jsonKonfig.encodeToString(UserProfile.serializer(), profil)).apply()
    }

    override suspend fun ladeProfil(): UserProfile? {
        val json = prefs.getString("profil", null) ?: return null
        return try { jsonKonfig.decodeFromString(UserProfile.serializer(), json) } catch (e: Exception) { null }
    }
}

actual fun provideRepository(): TrainingRepository = AndroidRepository()
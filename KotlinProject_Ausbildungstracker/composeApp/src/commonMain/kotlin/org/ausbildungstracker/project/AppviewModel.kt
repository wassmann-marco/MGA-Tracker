package org.ausbildungstracker.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString      // DIESE ZEILE HAT GEFEHLT!
import kotlinx.serialization.decodeFromString    // DIESE ZEILE HAT GEFEHLT!
import kotlinx.serialization.builtins.ListSerializer

class AppViewModel(private val repository: TrainingRepository) : ViewModel() {

    val eintragZumBearbeiten = MutableStateFlow<TrainingEintrag?>(null)
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // --- NEU: Das Gehirn der App wird beim Start aktiviert ---
    init {
        viewModelScope.launch {
            repository.laden() // Lädt Einträge
            val profil = repository.ladeProfil() // Lädt Profil
            if (profil != null) _userProfile.value = profil
        }
    }

    fun updateUserProfile(profile: UserProfile) = viewModelScope.launch {
        _userProfile.value = profile
        repository.speichereProfil(profile) // AUTO-SAVE Profil
    }

    fun exportiereDatenAlsJson(): String {
        return Json { prettyPrint = true }.encodeToString(ListSerializer(TrainingEintrag.serializer()), eintraege.value)
    }

    fun importiereDatenAusJson(json: String) = viewModelScope.launch {
        try {
            val importierteListe = Json { ignoreUnknownKeys = true }.decodeFromString(ListSerializer(TrainingEintrag.serializer()), json)
            importierteListe.forEach { repository.speichereEintrag(it) }
        } catch (e: Exception) { println("Fehler: ${e.message}") }
    }

    val eintraege: StateFlow<List<TrainingEintrag>> = repository.holeAlleEintraege()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bekannteAusbilder: StateFlow<List<String>> = eintraege.map { liste ->
        liste.map { it.ausbilder }.filter { it.isNotBlank() }.distinct().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedQs = MutableStateFlow(QS.QS1)

    fun neuerEintrag(modulId: String, datum: String, startH: Int, startM: Int, endeH: Int, endeM: Int, ausbilder: String, bemerkung: String, qs: String) = viewModelScope.launch {
        val idToUse = eintragZumBearbeiten.value?.id ?: aktuellesDatumMillis()
        val neu = TrainingEintrag(idToUse, modulId, datum, ausbilder, startH, startM, endeH, endeM, bemerkung)
        repository.speichereEintrag(neu)
        eintragZumBearbeiten.value = null
    }

    fun loeschen(id: Long) = viewModelScope.launch { repository.loescheEintrag(id) }

    fun getIstUE(eintraege: List<TrainingEintrag>, modulId: String): Double {
        val min = eintraege.filter { it.modulId == modulId }.sumOf {
            val s = it.startH * 60 + it.startM
            val e = it.endeH * 60 + it.endeM
            (e - s).toDouble().coerceAtLeast(0.0)
        }
        return min / 45.0
    }

    fun getAnrechenbareUE(eintraege: List<TrainingEintrag>, modul: Modul): Double = min(getIstUE(eintraege, modul.id), modul.sollStunden)

    fun gesamtFortschritt(eintraege: List<TrainingEintrag>): Float {
        val soll = alleModule.sumOf { it.sollStunden }
        val ist = alleModule.sumOf { getAnrechenbareUE(eintraege, it) }
        return if (soll > 0) (ist / soll).toFloat().coerceIn(0f, 1f) else 0f
    }

    fun qs1SchnellAusfuellen(heute: String) = viewModelScope.launch {
        alleModule.filter { it.qs == QS.QS1 }.forEach { modul ->
            if (getIstUE(eintraege.value, modul.id) < modul.sollStunden) {
                val h = (modul.sollStunden * 45 / 60).toInt()
                val m = (modul.sollStunden * 45 % 60).toInt()
                repository.speichereEintrag(TrainingEintrag(aktuellesDatumMillis() + modul.id.hashCode(), modul.id, heute, "Auto-Fill", 18, 0, 18 + h, m, "Automatisch"))
            }
        }
    }
}
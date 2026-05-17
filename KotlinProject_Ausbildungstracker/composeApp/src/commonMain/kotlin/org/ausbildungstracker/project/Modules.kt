package org.ausbildungstracker.project

enum class QS { QS1, QS2 }

data class Modul(
    val id: String,
    val name: String,
    val sollStunden: Double,
    val qs: QS
)

// Deine Liste der Module (unverändert übernommen)
val alleModule = listOf(
    Modul("1.2", "Unfallversicherung", 2.0, QS.QS1),
    Modul("3.1", "Fahrzeugkunde Theorie", 1.0, QS.QS1),
    Modul("4.1", "Persönliche & erweiterte Ausrüstung", 1.0, QS.QS1),
    Modul("4.2", "Löschgeräte, Schläuche, Armaturen", 2.0, QS.QS1),
    Modul("4.3", "Geräte für die einfache techn. Hilfeleistung", 3.0, QS.QS1),
    Modul("4.4", "Rettungsgeräte - Knoten und Stiche", 2.0, QS.QS1),
    Modul("4.5", "Rettungsgeräte - Leitern", 3.0, QS.QS1),
    Modul("4.6", "Rettungsgeräte - Sonstige", 2.0, QS.QS1),
    Modul("4.7", "Beleuchtungs- und Warngeräte", 3.0, QS.QS1),
    Modul("5.1", "Erste Hilfe", 9.0, QS.QS1),
    Modul("5.2", "Physische & psychische Belastungen im Einsatz", 3.0, QS.QS1),
    Modul("6.0", "Verhalten bei Gefahr", 4.0, QS.QS1),
    Modul("6.0-K", "Verhalten bei Gefahr (KatS)", 1.0, QS.QS1),
    Modul("1.1", "Organisation der Feuerwehr", 1.0, QS.QS2),
    Modul("2.0", "Brennen und Löschen", 3.0, QS.QS2),
    Modul("3.2", "Fahrzeugkunde Praxis", 3.0, QS.QS2),
    Modul("5.3", "Erste Hilfe Fortbildung", 9.0, QS.QS2),
    Modul("7.0", "Rettung (Technisch/Eis/Höhen/Tiefen)", 9.0, QS.QS2),
    Modul("7.0-K", "Rettung (KatS)", 1.0, QS.QS2),
    Modul("8.1", "Einheiten im Löscheinsatz - Praxis", 16.0, QS.QS2),
    Modul("8.2", "Einsatzübung Löscheinsatz", 30.0, QS.QS2),
    Modul("9.1", "Einheiten im Hilfeleistungseinsatz", 5.0, QS.QS2),
    Modul("9.2", "Einsatzübung Techn. Hilfe", 5.0, QS.QS2),
    Modul("9.2-K", "Einsatzübung Techn. Hilfe (KatS)", 3.0, QS.QS2),
    Modul("10.1", "ABC-Gefahrstoffe - Kennzeichnung", 3.0, QS.QS2),
    Modul("10.2", "ABC-Gefahrstoffe - Gefahren/Verhalten", 3.0, QS.QS2),
    Modul("11.0", "Sprechfunk-Einstiegsmodul", 8.0, QS.QS2),
    Modul("12.0", "Objektkunde", 5.0, QS.QS2),
    Modul("13.1-K", "Grundlagen Zivil- & KatS ", 2.0, QS.QS2),
    Modul("13.2-K", "Besondere Gefahren im Zivilschutz, Kampfmittel (KatS)", 3.0, QS.QS2),
    Modul("13.3-K", "Sonderfahrzeuge (KatS)", 2.0, QS.QS2)
)
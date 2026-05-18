package org.ausbildungstracker.project

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlinproject_ausbildungstracker.composeapp.generated.resources.Res
import kotlinproject_ausbildungstracker.composeapp.generated.resources.pferd
import kotlinproject_ausbildungstracker.composeapp.generated.resources.pferd2

// --- Zentrale Farbdefinitionen ---
val FeuerwehrRot  = Color(0xFF7D0B23)
val FeuerwehrGold = Color(0xFFFFCC00)
val HellGrau      = Color(0xFFF4F4F4)

enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Dashboard", Icons.Default.Home),
    PROGRESS("Modul-Check", Icons.Default.Star),
    ADD("Neuer Eintrag", Icons.Default.Add),
    PROFILE("Profil", Icons.Default.Person)
}

// --- Hilfsfunktion für das Datum (WasmJS sicher ohne java.time) ---
fun parseDateToEpochDays(dateStr: String): Long {
    if (dateStr.isBlank()) return 0L
    val parts = dateStr.split(".")
    if (parts.size != 3) return 0L
    try {
        val d = parts[0].toInt()
        val m = parts[1].toInt()
        val y = parts[2].toInt()

        var days = 0
        for (year in 1970 until y) {
            days += if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
        }
        val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) monthDays[2] = 29
        for (i in 1 until m) days += monthDays[i]
        days += (d - 1)
        return days.toLong()
    } catch(e: Exception) { return 0L }
}

@Composable
fun App() {
    val repository = remember { provideRepository() }
    val viewModel = remember { AppViewModel(repository) }

    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary    = FeuerwehrRot,
            secondary  = FeuerwehrGold,
            background = HellGrau,
            surface    = Color.White,
            onPrimary  = Color.White
        )
    ) {
        if (showSplash) {
            SplashScreen()
        } else {
            MainApp(viewModel)
        }
    }
}

@Composable
fun SplashScreen() {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FeuerwehrRot),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).padding(horizontal = 20.dp)
        ) {
            Text(text = "Ausbildungstracker für die", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text(text = "MGA", color = FeuerwehrGold, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = "in Niedersachsen", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(50.dp))
            Image(painterResource(Res.drawable.pferd), contentDescription = null, modifier = Modifier.size(120.dp))
            Spacer(Modifier.height(25.dp))
            Text("Retten · Löschen · Bergen · Schützen", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(vm: AppViewModel) {
    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(Tab.OVERVIEW) }
    var menuExpanded by remember { mutableStateOf(false) }
    val pendingImport by vm.pendingImport.collectAsState()

    if (pendingImport != null) {
        AlertDialog(
            onDismissRequest = { vm.abbrechenImport() },
            icon = { Icon(Icons.Default.Info, null, tint = FeuerwehrRot) },
            title = { Text("Daten importieren") },
            text = { Text("${pendingImport!!.size} Einträge gefunden.\n\n• Hinzufügen: Zusammenführen mit bestehenden Daten.\n• Ersetzen: Alle vorhandenen Daten werden gelöscht.") },
            confirmButton = {
                Button(
                    onClick = { vm.bestaetigeImportErsetzen() },
                    colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot)
                ) { Text("Ersetzen") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { vm.abbrechenImport() }) { Text("Abbrechen", color = Color.Gray) }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { vm.bestaetigeImportHinzufuegen() }) { Text("Hinzufügen", color = FeuerwehrRot) }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ausbildungs-Tracker", color = Color.White) },
                navigationIcon = {
                    Image(painterResource(Res.drawable.pferd2), contentDescription = null, modifier = Modifier.size(40.dp).padding(start = 8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FeuerwehrRot),
                actions = {
                    IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White) }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Daten sichern (Export)") },
                            onClick = { menuExpanded = false; downloadJsonDatei(vm.exportiereDatenAlsJson(), "MGA_Tracker_Backup.json") }
                        )
                        DropdownMenuItem(
                            text = { Text("Daten einspielen (Import)") },
                            onClick = { menuExpanded = false; scope.launch { pickAndImportJsonDatei { vm.importiereDatenAusJson(it) } } }
                        )
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Dienstnachweis (PDF)") } },
                            onClick = { menuExpanded = false; generierePdf(vm.eintraege.value, vm.userProfile.value) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick  = { if (tab == Tab.ADD) vm.eintragZumBearbeiten.value = null; currentTab = tab },
                        icon     = { Icon(tab.icon, contentDescription = null) },
                        label    = { Text(tab.label, fontSize = 11.sp) },
                        colors   = NavigationBarItemDefaults.colors(selectedIconColor = FeuerwehrRot, selectedTextColor = FeuerwehrRot, indicatorColor = FeuerwehrRot.copy(alpha = 0.12f))
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                Tab.OVERVIEW -> OverviewScreen(vm) { vm.selectedQs.value = it; currentTab = Tab.PROGRESS }
                Tab.PROGRESS -> ProgressScreen(vm) { currentTab = Tab.ADD }
                Tab.ADD -> AddEntryScreen(vm) { currentTab = Tab.PROGRESS }
                Tab.PROFILE -> ProfileScreen(vm)
            }
        }
    }
}

@Composable
fun CountdownCard(startDatum: String) {
    if (startDatum.isBlank()) return

    val maxTage = 3 * 365 // 1095 Tage (3 Jahre)
    val startTage = parseDateToEpochDays(startDatum)
    val heuteTage = aktuellesDatumMillis() / 86400000L // 86.400.000 ms pro Tag

    if (startTage == 0L) return

    val vergangen = heuteTage - startTage
    val verbleibend = maxTage - vergangen
    val progress = (vergangen.toFloat() / maxTage.toFloat()).coerceIn(0f, 1f)

    val restText = when {
        verbleibend > 30 -> "${verbleibend / 30} Monate, ${verbleibend % 30} Tage"
        verbleibend > 0 -> "$verbleibend Tage"
        else -> "Zeit abgelaufen!"
    }

    val barColor = when {
        progress < 0.5f -> Color(0xFF2E7D32)
        progress < 0.8f -> FeuerwehrGold
        else -> FeuerwehrRot
    }
    val icon = if (progress >= 0.8f) Icons.Default.Warning else Icons.Default.DateRange

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = barColor)
                Spacer(Modifier.width(8.dp))
                Text("Verbleibende Ausbildungszeit", fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(restText, fontSize = 20.sp, fontWeight = FontWeight.Black, color = barColor)
                Text("Max. 3 Jahre", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = barColor,
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
fun OverviewScreen(vm: AppViewModel, onNavigateToProgress: (QS) -> Unit = {}) {
    val eintraege by vm.eintraege.collectAsState()
    val profile by vm.userProfile.collectAsState()
    val scrollState = rememberScrollState()

    fun getAnrechenbareUE(modul: Modul): Double {
        val istUE = vm.getIstUE(eintraege, modul.id)
        return if (istUE > modul.sollStunden) modul.sollStunden else istUE
    }

    val qs1Module = alleModule.filter { it.qs == QS.QS1 }
    val qs1Ist = qs1Module.sumOf { getAnrechenbareUE(it) }
    val qs1Soll = qs1Module.sumOf { it.sollStunden }
    val qs1Prozent = if (qs1Soll > 0) (qs1Ist / qs1Soll).toFloat().coerceIn(0f, 1f) else 0f

    val qs2Module = alleModule.filter { it.qs == QS.QS2 }
    val qs2Ist = qs2Module.sumOf { getAnrechenbareUE(it) }
    val qs2Soll = qs2Module.sumOf { it.sollStunden }
    val qs2Prozent = if (qs2Soll > 0) (qs2Ist / qs2Soll).toFloat().coerceIn(0f, 1f) else 0f

    val gesamtProzent = vm.gesamtFortschritt(eintraege)

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(horizontal = 16.dp).verticalScroll(scrollState)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
        Text("MGA Niedersachsen", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = FeuerwehrRot), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Text("Gesamtfortschritt", color = Color.White, fontWeight = FontWeight.Bold)
                Text("${(gesamtProzent * 100).toInt()}%", color = FeuerwehrGold, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                LinearProgressIndicator(progress = { gesamtProzent }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)), color = FeuerwehrGold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatsBarCard(eintraege, vm)
        Spacer(modifier = Modifier.height(12.dp))

        Text("Qualifikationsstufen", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        QSCard("QS 1 Einsatzfähigkeit", qs1Ist, qs1Soll, qs1Prozent) { onNavigateToProgress(QS.QS1) }
        Spacer(modifier = Modifier.height(12.dp))
        QSCard("QS 2 Truppmitglied", qs2Ist, qs2Soll, qs2Prozent) { onNavigateToProgress(QS.QS2) }

        val letzteEintraege = eintraege.sortedByDescending { parseDateToEpochDays(it.datum) }.take(3)
        if (letzteEintraege.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Zuletzt eingetragen", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            letzteEintraege.forEach { e ->
                val modulName = alleModule.find { it.id == e.modulId }?.name ?: e.modulId
                val ue = ((e.endeH * 60 + e.endeM) - (e.startH * 60 + e.startM)) / 45.0
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${e.datum} • ${e.ausbilder}", fontSize = 12.sp, color = Color.Gray)
                            Text(modulName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${formatUE(ue)} UE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        CountdownCard(profile.ausbildungsstart)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun StatsBarCard(eintraege: List<TrainingEintrag>, vm: AppViewModel) {
    val abgeschlossen = alleModule.count { modul -> vm.getIstUE(eintraege, modul.id) >= modul.sollStunden }
    val gesamtModule = alleModule.size
    val anrechenbarUE = alleModule.sumOf { modul ->
        val ist = vm.getIstUE(eintraege, modul.id)
        if (ist > modul.sollStunden) modul.sollStunden else ist
    }
    val tatsaechlichUE = alleModule.sumOf { modul -> vm.getIstUE(eintraege, modul.id) }
    val maxUE = alleModule.sumOf { it.sollStunden }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (abgeschlossen == gesamtModule) Color(0xFF2E7D32) else FeuerwehrRot, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text("$abgeschlossen / $gesamtModule", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.DarkGray)
                Text("Module", fontSize = 11.sp, color = Color.Gray)
            }
            Box(modifier = Modifier.width(1.dp).height(72.dp).align(Alignment.CenterVertically).background(Color.LightGray))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = FeuerwehrGold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text("${formatUE(anrechenbarUE)} / ${maxUE.toInt()} UE", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.DarkGray)
                Text("anrechenbar", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Text("${formatUE(tatsaechlichUE)} UE geleistet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
        }
    }
}

@Composable
fun QSCard(titel: String, ist: Double, soll: Double, progress: Float, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titel, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
                Text("${formatUE(ist)} / ${soll.toInt()} UE", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = FeuerwehrRot)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(vm: AppViewModel, onSaved: () -> Unit) {
    val scrollState = rememberScrollState()
    val zuBearbeiten by vm.eintragZumBearbeiten.collectAsState()
    var datum by remember { mutableStateOf("") }
    var startStunde by remember { mutableStateOf(16) }
    var startMinute by remember { mutableStateOf(0) }
    var endeStunde by remember { mutableStateOf(18) }
    var endeMinute by remember { mutableStateOf(0) }
    var ausgewähltesModul by remember { mutableStateOf(alleModule[0]) }
    var ausbilder by remember { mutableStateOf("") }
    var bemerkung by remember { mutableStateOf("") }
    var expandedModul by remember { mutableStateOf(false) }
    val bekannteAusbilder by vm.bekannteAusbilder.collectAsState()
    var showVorschlaege by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(zuBearbeiten) {
        if (zuBearbeiten != null) {
            val e = zuBearbeiten!!
            datum = e.datum
            startStunde = e.startH
            startMinute = e.startM
            endeStunde = e.endeH
            endeMinute = e.endeM
            ausgewähltesModul = alleModule.find { it.id == e.modulId } ?: alleModule[0]
            ausbilder = e.ausbilder
            bemerkung = e.bemerkung
        } else {
            datum = ""; startStunde = 16; startMinute = 0; endeStunde = 18; endeMinute = 0
            ausgewähltesModul = alleModule[0]; ausbilder = ""; bemerkung = ""
        }
    }

    val dauerMinuten = (endeStunde * 60 + endeMinute) - (startStunde * 60 + startMinute)
    val dauerText = if (dauerMinuten > 0) "Dauer: ${dauerMinuten / 60}h" + if(dauerMinuten % 60 != 0) " ${dauerMinuten % 60}min" else "" else "Ungültiger Zeitraum"

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = aktuellesDatumMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { datum = formatiereDatum(it) }; showDatePicker = false }) { Text("OK", color = FeuerwehrRot) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen", color = Color.Gray) } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = startStunde, initialMinute = startMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            confirmButton = { TextButton(onClick = { startStunde = timePickerState.hour; startMinute = timePickerState.minute; showStartTimePicker = false }) { Text("OK", color = FeuerwehrRot) } },
            dismissButton = { TextButton(onClick = { showStartTimePicker = false }) { Text("Abbrechen", color = Color.Gray) } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = endeStunde, initialMinute = endeMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            confirmButton = { TextButton(onClick = { endeStunde = timePickerState.hour; endeMinute = timePickerState.minute; showEndTimePicker = false }) { Text("OK", color = FeuerwehrRot) } },
            dismissButton = { TextButton(onClick = { showEndTimePicker = false }) { Text("Abbrechen", color = Color.Gray) } },
            text = { TimePicker(state = timePickerState) }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp).verticalScroll(scrollState)) {
        Text(if (zuBearbeiten != null) "Eintrag bearbeiten" else "Neuer Eintrag", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
        Spacer(Modifier.height(24.dp))
        Text("Datum", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        OutlinedTextField(value = datum, onValueChange = {}, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, readOnly = true, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledTrailingIconColor = FeuerwehrRot), shape = RoundedCornerShape(8.dp), placeholder = { Text("z.B. 17.04.2026") }, trailingIcon = { Icon(Icons.Default.DateRange, null) })
        Spacer(Modifier.height(24.dp))
        Text("Zeitraum", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    TimeDisplayColumn("Von", startStunde, startMinute) { showStartTimePicker = true }
                    Text("—", fontSize = 24.sp, color = Color.LightGray)
                    TimeDisplayColumn("Bis", endeStunde, endeMinute) { showEndTimePicker = true }
                }
                Spacer(Modifier.height(12.dp))
                Text(dauerText, color = if(dauerMinuten > 0) Color(0xFF2E7D32) else FeuerwehrRot, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Modul", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        ExposedDropdownMenuBox(expanded = expandedModul, onExpandedChange = { expandedModul = !expandedModul }) {
            OutlinedTextField(value = "${ausgewähltesModul.id} – ${ausgewähltesModul.name}", onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedModul) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(8.dp))
            ExposedDropdownMenu(expanded = expandedModul, onDismissRequest = { expandedModul = false }) {
                alleModule.forEach { modul -> DropdownMenuItem(text = { Text("${modul.id} – ${modul.name}") }, onClick = { ausgewähltesModul = modul; expandedModul = false }) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Verantwortlicher Ausbilder", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        val gefilterteVorschlaege = bekannteAusbilder.filter { it.contains(ausbilder, ignoreCase = true) && it != ausbilder }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = ausbilder, onValueChange = { ausbilder = it; showVorschlaege = true }, placeholder = { Text("Name des Ausbilders") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), singleLine = true)
            DropdownMenu(expanded = showVorschlaege && gefilterteVorschlaege.isNotEmpty(), onDismissRequest = { showVorschlaege = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                gefilterteVorschlaege.forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { ausbilder = name; showVorschlaege = false }) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Bemerkung (optional)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        OutlinedTextField(value = bemerkung, onValueChange = { bemerkung = it }, placeholder = { Text("Zusatzinfos, z.B. Thema der Ausbildung") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), minLines = 2, maxLines = 5)
        Spacer(modifier = Modifier.height(32.dp))
        val istFertigAusgefuellt = datum.isNotBlank() && ausbilder.isNotBlank() && dauerMinuten > 0
        Button(onClick = { vm.neuerEintrag(modulId = ausgewähltesModul.id, datum = datum, startH = startStunde, startM = startMinute, endeH = endeStunde, endeM = endeMinute, ausbilder = ausbilder, bemerkung = bemerkung, qs = ausgewähltesModul.qs.name); onSaved() }, modifier = Modifier.fillMaxWidth().height(50.dp), enabled = istFertigAusgefuellt, colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot, disabledContainerColor = Color.LightGray), shape = RoundedCornerShape(8.dp)) {
            Text(text = if (zuBearbeiten != null) "Änderungen speichern" else "Eintrag speichern", color = if (istFertigAusgefuellt) Color.White else Color.DarkGray, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun TimeDisplayColumn(label: String, h: Int, m: Int, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { TimeBox(h.toString().padStart(2, '0')); Text(" : ", fontWeight = FontWeight.Bold, fontSize = 20.sp); TimeBox(m.toString().padStart(2, '0')) }
    }
}

@Composable
fun TimeBox(text: String) {
    Box(modifier = Modifier.size(width = 45.dp, height = 55.dp).background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) { Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot) }
}

@Composable
fun ProgressScreen(vm: AppViewModel, onNavigateToAdd: () -> Unit) {
    val eintraege by vm.eintraege.collectAsState()
    val selectedQs by vm.selectedQs.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    fun getStatsForStufe(stufe: QS): Pair<Float, Double> {
        val module = alleModule.filter { it.qs == stufe }
        val soll = module.sumOf { it.sollStunden }
        val istGedeckelt = module.sumOf { m -> val ist = vm.getIstUE(eintraege, m.id); if (ist > m.sollStunden) m.sollStunden else ist }
        val prozent = if (soll > 0) (istGedeckelt / soll).toFloat().coerceIn(0f, 1f) else 0f
        return Pair(prozent, soll)
    }

    val statsQS1 = getStatsForStufe(QS.QS1)
    val statsQS2 = getStatsForStufe(QS.QS2)
    val moduleDerStufe = alleModule.filter { it.qs == selectedQs }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false }, icon = { Icon(Icons.Default.Warning, null, tint = FeuerwehrRot) }, title = { Text("QS 1 abschließen?") }, text = { Text("Möchtest du alle fehlenden Module der QS 1 automatisch als abgeschlossen markieren?") },
            confirmButton = { Button(onClick = { vm.qs1SchnellAusfuellen(formatiereDatum(aktuellesDatumMillis())); showDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Text("Ja, abschließen") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Abbrechen", color = Color.Gray) } }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)).padding(16.dp)) {
        Text("Modul-Check", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QSSelectorBox("QS 1", statsQS1.first, selectedQs == QS.QS1, Modifier.weight(1f)) { vm.selectedQs.value = QS.QS1 }
            QSSelectorBox("QS 2", statsQS2.first, selectedQs == QS.QS2, Modifier.weight(1f)) { vm.selectedQs.value = QS.QS2 }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Module für ${if(selectedQs == QS.QS1) "Einsatzfähigkeit" else "Truppmitglied"}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(moduleDerStufe) { modul -> ModulItem(modul, vm, eintraege, onNavigateToAdd) }
            if (selectedQs == QS.QS1 && statsQS1.first < 1f) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp).clickable { showDialog = true }, colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(Icons.Default.CheckCircle, null, tint = FeuerwehrGold); Spacer(Modifier.width(12.dp)); Text("Alle QS 1 Module abschließen", color = FeuerwehrGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
            }
        }
    }
}

@Composable
fun QSSelectorBox(label: String, prozent: Float, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bgColor = if (isSelected) FeuerwehrRot else Color.White
    val highlightColor = if (isSelected) FeuerwehrGold else FeuerwehrRot
    val labelColor = if (isSelected) FeuerwehrGold else Color.DarkGray
    val borderColor = if (isSelected) FeuerwehrGold else Color.LightGray
    val statusIcon = when {
        prozent >= 1f -> Icons.Default.SentimentVerySatisfied
        prozent >= 0.5f -> Icons.Default.SentimentSatisfied
        prozent >= 0.2f -> Icons.Default.SentimentNeutral
        else -> Icons.Default.SentimentDissatisfied
    }
    Card(modifier = modifier.height(120.dp).clickable { onClick() }.border(2.dp, borderColor, RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = bgColor), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = labelColor); Spacer(Modifier.width(10.dp)); Icon(imageVector = statusIcon, contentDescription = null, tint = highlightColor, modifier = Modifier.size(28.dp)) }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${(prozent * 100).toInt()}%", fontWeight = FontWeight.Black, fontSize = 28.sp, color = highlightColor)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { prozent }, modifier = Modifier.fillMaxWidth(0.85f).height(8.dp).clip(RoundedCornerShape(50)), color = highlightColor, trackColor = if (isSelected) Color.White.copy(alpha = 0.3f) else Color.LightGray)
        }
    }
}

@Composable
fun ModulItem(modul: Modul, vm: AppViewModel, eintraege: List<TrainingEintrag>, onNavigateToAdd: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var zuLoeschenId by remember { mutableStateOf<Long?>(null) }

    if (zuLoeschenId != null) {
        AlertDialog(
            onDismissRequest = { zuLoeschenId = null },
            icon = { Icon(Icons.Default.Warning, null, tint = FeuerwehrRot) },
            title = { Text("Eintrag löschen?") },
            text = { Text("Dieser Eintrag wird unwiderruflich gelöscht.") },
            confirmButton = {
                Button(
                    onClick = { vm.loeschen(zuLoeschenId!!); zuLoeschenId = null },
                    colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot)
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { zuLoeschenId = null }) { Text("Abbrechen", color = Color.Gray) } }
        )
    }
    val istUE = vm.getIstUE(eintraege, modul.id)
    val sollUE = modul.sollStunden
    val progress = if (sollUE > 0) (istUE / sollUE).toFloat().coerceIn(0f, 1f) else 0f
    val isDone = istUE >= sollUE

    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) { Text("Modul ${modul.id}", fontSize = 11.sp, color = Color.Gray); Text(modul.name, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                if (isDone) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32))
                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(4.dp)), color = if (isDone) Color(0xFF2E7D32) else FeuerwehrRot)
                Spacer(modifier = Modifier.width(8.dp)); Text("${formatUE(istUE)} / ${sollUE.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            androidx.compose.animation.AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
                    val modulEintraege = eintraege.filter { it.modulId == modul.id }.sortedByDescending { parseDateToEpochDays(it.datum) }
                    if (modulEintraege.isEmpty()) { Text("Keine Einträge", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp)) }
                    else {
                        modulEintraege.forEach { e ->
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val startZeit = "${e.startH.toString().padStart(2, '0')}:${e.startM.toString().padStart(2, '0')}"
                                    val endeZeit = "${e.endeH.toString().padStart(2, '0')}:${e.endeM.toString().padStart(2, '0')}"
                                    Text("${e.datum} • $startZeit - $endeZeit Uhr • Ausbilder: ${e.ausbilder}", fontSize = 12.sp)
                                    if (e.bemerkung.isNotBlank()) { Text(e.bemerkung, fontSize = 10.sp, color = Color.Gray) }
                                }
                                val ue = ((e.endeH * 60 + e.endeM) - (e.startH * 60 + e.startM)) / 45.0
                                Text("${formatUE(ue)} UE", fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.width(12.dp))
                                IconButton(onClick = { vm.eintragZumBearbeiten.value = e; onNavigateToAdd() }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, null, tint = FeuerwehrGold, modifier = Modifier.size(18.dp)) }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { zuLoeschenId = e.id }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, tint = FeuerwehrRot, modifier = Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(vm: AppViewModel) {
    val profile by vm.userProfile.collectAsState()
    var vorname by remember { mutableStateOf(profile.vorname) }
    var nachname by remember { mutableStateOf(profile.name) }
    var ortswehr by remember { mutableStateOf(profile.ortsfeuerwehr) }
    var startDatum by remember { mutableStateOf(profile.ausbildungsstart) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(profile) { vorname = profile.vorname; nachname = profile.name; ortswehr = profile.ortsfeuerwehr; startDatum = profile.ausbildungsstart }

    if (showDatePicker) {
        val initialMillis = if (startDatum.isNotBlank()) parseDateToEpochDays(startDatum) * 86400000L else aktuellesDatumMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { startDatum = formatiereDatum(it) }; showDatePicker = false }) { Text("OK", color = FeuerwehrRot) } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen", color = Color.Gray) } }
        ) { DatePicker(state = datePickerState) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Benutzerprofil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = FeuerwehrRot, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(value = vorname, onValueChange = { vorname = it }, label = { Text("Vorname") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = nachname, onValueChange = { nachname = it }, label = { Text("Nachname") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = ortswehr, onValueChange = { ortswehr = it }, label = { Text("Ortsfeuerwehr") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = startDatum, onValueChange = {}, label = { Text("Ausbildungsstart") }, modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }, readOnly = true, enabled = false, colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.Gray, disabledTrailingIconColor = FeuerwehrRot), trailingIcon = { Icon(Icons.Default.DateRange, null) }, shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { vm.updateUserProfile(UserProfile(vorname, nachname, ortswehr, startDatum)) }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = FeuerwehrRot), shape = RoundedCornerShape(8.dp)) { Text("Profil speichern", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) }
        Spacer(modifier = Modifier.height(32.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = HellGrau), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Über den Ausbildungs-Tracker", fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Ich hoffe diese App kann euch dabei unterstützen, euren Ausbildungsstand im Blick zu behalten.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Entwickelt von:", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(text = "Marco Waßmann / FF Hehlen", fontWeight = FontWeight.Bold, color = FeuerwehrRot, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(text = "Version 1.22", fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = HellGrau), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = Color.Gray); Spacer(modifier = Modifier.width(8.dp)); Text("Gut zu wissen", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Dieser Tracker ist ein Hilfsmittel für dich, kein offizielles Dokument. Maßgeblich für deinen Ausbildungsstand sind allein deine Daten in FeuerON bzw. im Dienstbuch.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}
package com.example.rippleeffect

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "RippleEffect"

object AppColors {
    val Primary = Color(0xFF613DC7)
    val Background = Color(0xFFFFFFFF)
    val DarkBackground = Color(0xFF131315)
    val Surface = Color.White
    val TextPrimary = Color(0xFF0D0D0D)
    val TextSecondary = Color(0xFF707070)
    val Border = Color(0xFFEFEFEF)
    val Accent = Color(0xFF613DC7)
    val Gray50 = Color(0xFFFAFAFA)
    val Blue100 = Color(0xFFE0E9F3)
    val Blue800 = Color(0xFF1D4ED8)
    val RetryBlue = Color(0xFF0077EE)
    val UserIconBg = Color(0xFFFFE4DD)
    val UserIconText = Color(0xFFFF7A56)
    val SuccessGreen = Color(0xFF10B97A)
    val ErrorRed = Color(0xFFF0554C)
    val WarningYellow = Color(0xFFFBBF24)
    val CardBackground = Color(0xFFF5F5F5)
}

data class AccountingEntry(val id: String = UUID.randomUUID().toString(), var type: String, var amount: Double, val isExpense: Boolean)
data class SavedReport(val id: String = UUID.randomUUID().toString(), var name: String, val dateCreated: Date, var status: ReportStatus, val owner: String, var content: String = "")
enum class ReportStatus { COMPLETED, IN_PROGRESS, DRAFT }
enum class Currency(val code: String, val symbol: String, val nameEn: String, val nameEs: String) {
    USD("USD", "$", "US Dollar", "Dólar EEUU"),
    GTQ("GTQ", "Q", "Quetzal", "Quetzal"),
    HNL("HNL", "L", "Lempira", "Lempira")
}
enum class AppLifecycle { INITIALIZING, READY, ERROR }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RippleEffectTheme { RippleEffectApp() } }
    }
}

@Composable
fun RippleEffectTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = AppColors.Primary, background = AppColors.Background, surface = AppColors.Surface), content = content)
}

@Composable
fun RippleEffectApp() {
    var lifecycleState by remember { mutableStateOf(AppLifecycle.INITIALIZING) }
    var selectedTab by remember { mutableStateOf(2) }
    var language by remember { mutableStateOf("en") }
    var translation by remember { mutableStateOf("Hello, how are you?") }
    val accountingEntries = remember { mutableStateListOf<AccountingEntry>() }
    val savedReports = remember { mutableStateListOf(
        SavedReport(name = "Annual Audit", dateCreated = Date(), status = ReportStatus.COMPLETED, owner = "John Doe", content = "Initial report content..."),
        SavedReport(name = "Q3 Summary", dateCreated = Date(System.currentTimeMillis() - 86400000), status = ReportStatus.IN_PROGRESS, owner = "Jane Smith", content = "Quarterly results...")
    ) }

    LaunchedEffect(Unit) { delay(1500); lifecycleState = AppLifecycle.READY }

    Crossfade(targetState = lifecycleState, label = "AppTransition") { state ->
        when (state) {
            AppLifecycle.INITIALIZING -> LoadingScreen()
            AppLifecycle.ERROR -> RetryScreen { lifecycleState = AppLifecycle.INITIALIZING }
            AppLifecycle.READY -> MainContent(
                selectedTab = selectedTab, onTabSelected = { selectedTab = it },
                language = language, onLanguageChange = { lang -> language = lang; translation = if (lang == "es") "Hola, ¿cómo estás?" else "Hello, how are you?" },
                translation = translation, entries = accountingEntries, reports = savedReports
            )
        }
    }
}

@Composable
fun LoadingScreen() { Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 2.dp) } }

@Composable
fun RetryScreen(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(AppColors.DarkBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Could not load network resources", color = Color(0xFFD0D0D0), fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppColors.RetryBlue)) { Text("Retry", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(selectedTab: Int, onTabSelected: (Int) -> Unit, language: String, onLanguageChange: (String) -> Unit, translation: String, entries: MutableList<AccountingEntry>, reports: MutableList<SavedReport>) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Text("📚", fontSize = 24.sp); Spacer(Modifier.width(12.dp)); Text("Ripple Effect", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) } },
                actions = { Box(Modifier.padding(end = 16.dp).size(32.dp).clip(CircleShape).background(AppColors.UserIconBg), contentAlignment = Alignment.Center) { Text("e", color = AppColors.UserIconText, fontWeight = FontWeight.Bold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 0.dp) {
                NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text("Items") }, selected = selectedTab == 0, onClick = { onTabSelected(0) })
                NavigationBarItem(icon = { Icon(Icons.Default.Folder, null) }, label = { Text("Categories") }, selected = selectedTab == 1, onClick = { onTabSelected(1) })
                NavigationBarItem(icon = { Icon(Icons.Default.Assessment, null) }, label = { Text("Project management") }, selected = selectedTab == 2, onClick = { onTabSelected(2) })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            when (selectedTab) {
                0 -> WaterSalesAccountingScreen(language == "es", entries, onReportSubmit = { newReport -> reports.add(0, newReport); onTabSelected(1) })
                1 -> CategoriesWithReportsScreen(language == "es", reports)
                2 -> ProjectManagementView(language, onLanguageChange, translation)
            }
        }
    }
}

// ==================== FEATURE: WATER SALES ACCOUNTING (ITEMS TAB) ====================
@Composable
fun WaterSalesAccountingScreen(isSpanish: Boolean, entries: MutableList<AccountingEntry>, onReportSubmit: (SavedReport) -> Unit) {
    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    val cost = entries.filter { it.isExpense }.sumOf { it.amount }
    val profit = entries.filter { !it.isExpense }.sumOf { it.amount }
    var transactionDate by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") }
    var summaryReport by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("") }
    var entryAmount by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(if (isSpanish) "Contabilidad de Ventas de Agua" else "Water Sales Accounting", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountingSummaryCard(Modifier.weight(1f), if (isSpanish) "Costos" else "Costs", cost, selectedCurrency, Color(0xFFFEE2E2), Color(0xFF991B1B))
                AccountingSummaryCard(Modifier.weight(1f), if (isSpanish) "Ganancias" else "Profit", profit, selectedCurrency, Color(0xFFDCFCE7), Color(0xFF166534))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isSpanish) "Seleccionar Moneda" else "Select Currency", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Currency.values().forEach { curr ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedCurrency = curr }.padding(vertical = 4.dp)) {
                            RadioButton(selected = selectedCurrency == curr, onClick = { selectedCurrency = curr })
                            Text("${curr.symbol} ${curr.code} (${if (isSpanish) curr.nameEs else curr.nameEn})", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isSpanish) "Registro de Datos" else "Data Entry", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = entryType, onValueChange = { entryType = it }, label = { Text(if (isSpanish) "Descripción" else "Description") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = entryAmount, onValueChange = { entryAmount = it }, label = { Text(if (isSpanish) "Cantidad" else "Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (entryType.isNotBlank()) { entries.add(AccountingEntry(type = entryType, amount = entryAmount.toDoubleOrNull() ?: 0.0, isExpense = true)); entryType = ""; entryAmount = "" } }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ErrorRed), modifier = Modifier.weight(1f)) { Text(if (isSpanish) "+ Gasto" else "+ Expense") }
                        Button(onClick = { if (entryType.isNotBlank()) { entries.add(AccountingEntry(type = entryType, amount = entryAmount.toDoubleOrNull() ?: 0.0, isExpense = false)); entryType = ""; entryAmount = "" } }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.SuccessGreen), modifier = Modifier.weight(1f)) { Text(if (isSpanish) "+ Venta" else "+ Sale") }
                    }
                    if (entries.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(if (isSpanish) "Registros Recientes" else "Recent Records", fontWeight = FontWeight.Bold)
                        entries.forEach { entry ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.type, Modifier.weight(1f), fontSize = 14.sp)
                                Text("${selectedCurrency.symbol}${entry.amount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                IconButton(onClick = { entryType = entry.type; entryAmount = entry.amount.toString(); entries.remove(entry) }) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)) }
                                IconButton(onClick = { entries.remove(entry) }) { Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = AppColors.ErrorRed) }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSpanish) "Informe" else "Report", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedButton(onClick = {}) { Icon(Icons.Default.Print, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (isSpanish) "Imprimir" else "Print") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth().background(AppColors.CardBackground, RoundedCornerShape(8.dp)).padding(16.dp)) {
                        ReportRow(if (isSpanish) "Costos Totales:" else "Total Costs:", "${selectedCurrency.symbol}${String.format("%.2f", cost)}")
                        ReportRow(if (isSpanish) "Ganancias Totales:" else "Total Profit:", "${selectedCurrency.symbol}${String.format("%.2f", profit)}")
                        ReportRow(if (isSpanish) "Beneficio Neto:" else "Net Benefit:", "${selectedCurrency.symbol}${String.format("%.2f", profit - cost)}")
                        if (entries.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp)); Text(if (isSpanish) "Desglose" else "Breakdown", fontWeight = FontWeight.Bold)
                            entries.forEach { entry -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.type, fontSize = 12.sp, color = AppColors.TextSecondary); Text("${if (entry.isExpense) "-" else "+"}${selectedCurrency.symbol}${entry.amount}", fontSize = 12.sp, color = if (entry.isExpense) AppColors.ErrorRed else AppColors.SuccessGreen) } }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(if (isSpanish) "Resumen del Informe (opcional)" else "Summary Report (optional)", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = summaryReport, onValueChange = { summaryReport = it }, placeholder = { Text("Write your summary report here...") }, modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp))
                }
            }
        }
        item {
            Button(
                onClick = {
                    val reportContent = buildString {
                        append("Accounting Summary:\n")
                        append("Total Costs: ${selectedCurrency.symbol}${String.format("%.2f", cost)}\n")
                        append("Total Profit: ${selectedCurrency.symbol}${String.format("%.2f", profit)}\n")
                        append("Net: ${selectedCurrency.symbol}${String.format("%.2f", profit - cost)}\n\n")
                        append("Breakdown:\n")
                        entries.forEach { append("- ${it.type}: ${if (it.isExpense) "-" else "+"}${selectedCurrency.symbol}${it.amount}\n") }
                        append("\nUser Summary:\n$summaryReport")
                    }
                    onReportSubmit(SavedReport(name = "Report ${SimpleDateFormat("MM/dd/yy").format(Date())}", dateCreated = Date(), status = ReportStatus.COMPLETED, owner = "Current User", content = reportContent))
                    entries.clear(); summaryReport = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(8.dp)
            ) { Text(if (isSpanish) "Enviar Informe" else "Submit Report") }
        }
    }
}

@Composable
fun AccountingSummaryCard(modifier: Modifier, title: String, amount: Double, currency: Currency, bgColor: Color, textColor: Color) {
    Card(modifier = modifier, border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, color = AppColors.TextSecondary)
            Text("${currency.symbol}${String.format("%.2f", amount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.background(bgColor, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(if (title == "Costs" || title == "Costos") "Expenses" else "Income", fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String) {
    Column { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = AppColors.TextSecondary, fontSize = 14.sp); Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AppColors.TextPrimary) }; HorizontalDivider(color = AppColors.Border) }
}

// ==================== FEATURE: CATEGORIES WITH ADVANCED EDITOR ====================
@Composable
fun CategoriesWithReportsScreen(isSpanish: Boolean, reports: MutableList<SavedReport>) {
    var selectedReport by remember { mutableStateOf<SavedReport?>(null) }
    var reportToDelete by remember { mutableStateOf<SavedReport?>(null) }

    if (selectedReport != null) {
        ReportEditorView(report = selectedReport!!, isSpanish = isSpanish,
            onExit = { if (selectedReport!!.status != ReportStatus.COMPLETED) selectedReport!!.status = ReportStatus.IN_PROGRESS; selectedReport = null },
            onComplete = { selectedReport!!.status = ReportStatus.COMPLETED; selectedReport = null }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = { val newReport = SavedReport(name = "Untitled Report", dateCreated = Date(), status = ReportStatus.IN_PROGRESS, owner = "Current User"); reports.add(0, newReport); selectedReport = newReport },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(if (isSpanish) "Nuevo Informe" else "Add New Report") }
            Text(if (isSpanish) "Informes Guardados" else "Saved Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(reports) { report -> ReportCardItem(report = report, isSpanish = isSpanish, onClick = { selectedReport = report }, onLongClick = { reportToDelete = report }) } }
        }
    }
    if (reportToDelete != null) {
        AlertDialog(onDismissRequest = { reportToDelete = null }, title = { Text(if (isSpanish) "Eliminar Informe" else "Delete Report") },
            text = { Text(if (isSpanish) "ADVERTENCIA: ¿Está seguro de que desea eliminar este informe para siempre? No podrá recuperarlo." else "WARNING: Are you sure you want to delete this report forever? You can never retrieve it again.") },
            confirmButton = { TextButton(onClick = { reports.remove(reportToDelete); reportToDelete = null }) { Text(if (isSpanish) "ELIMINAR" else "DELETE", color = AppColors.ErrorRed) } },
            dismissButton = { TextButton(onClick = { reportToDelete = null }) { Text(if (isSpanish) "CANCELAR" else "CANCEL") } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportCardItem(report: SavedReport, isSpanish: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(report.name, fontWeight = FontWeight.Bold); Text(report.owner, fontSize = 12.sp, color = AppColors.TextSecondary) }
            StatusBadge(report.status, isSpanish)
        }
    }
}

@Composable
fun ReportEditorView(report: SavedReport, isSpanish: Boolean, onExit: () -> Unit, onComplete: () -> Unit) {
    var isEditMode by remember { mutableStateOf(report.status == ReportStatus.IN_PROGRESS && report.content.isEmpty()) }
    var reportName by remember { mutableStateOf(report.name) }
    var reportContent by remember { mutableStateOf(report.content) }
    var fontSize by remember { mutableStateOf(16f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    BackHandler { onExit() }
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = if (isEditMode) AppColors.Blue100 else AppColors.Gray50, shadowElevation = 2.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = if (isEditMode) (if (isSpanish) "Modo Edición Activado" else "Editing Enabled") else (if (isSpanish) "¿Habilitar edición?" else "Enable editing?"), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Print logic */ }) { Icon(Icons.Default.Print, null, tint = AppColors.TextPrimary) }
                    Spacer(Modifier.width(8.dp))
                    if (!isEditMode) { Button(onClick = { isEditMode = true }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) { Text(if (isSpanish) "Editar" else "Edit") } }
                    else { TextButton(onClick = { isEditMode = false }) { Text(if (isSpanish) "Ver" else "View") } }
                }
            }
        }
        AnimatedVisibility(visible = isEditMode) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(AppColors.Gray50).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { isBold = !isBold }) { Icon(Icons.Default.FormatBold, null, tint = if (isBold) AppColors.Primary else AppColors.TextPrimary) }
                IconButton(onClick = { isItalic = !isItalic }) { Icon(Icons.Default.FormatItalic, null, tint = if (isItalic) AppColors.Primary else AppColors.TextPrimary) }
                IconButton(onClick = { isUnderline = !isUnderline }) { Icon(Icons.Default.FormatUnderlined, null, tint = if (isUnderline) AppColors.Primary else AppColors.TextPrimary) }
                IconButton(onClick = { fontSize += 2 }) { Icon(Icons.Default.TextIncrease, null) }
                IconButton(onClick = { if (fontSize > 8) fontSize -= 2 }) { Icon(Icons.Default.TextDecrease, null) }
                IconButton(onClick = { }) { Icon(Icons.Default.GridOn, null) }
                IconButton(onClick = { }) { Icon(Icons.Default.Image, null) }
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            if (isEditMode) {
                OutlinedTextField(value = reportName, onValueChange = { reportName = it; report.name = it }, label = { Text(if (isSpanish) "Título del informe" else "Report Title") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = reportContent, onValueChange = { reportContent = it; report.content = it }, placeholder = { Text(if (isSpanish) "Empieza a escribir..." else "Start typing your report...") }, modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal, textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None))
            } else { Text(reportName, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); Text(reportContent, fontSize = fontSize.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text(if (isSpanish) "Atrás" else "Back") }
            Button(onClick = onComplete, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.SuccessGreen)) { Text(if (isSpanish) "Publicar" else "Complete") }
        }
    }
}

@Composable
fun StatusBadge(status: ReportStatus, isSpanish: Boolean) {
    val color = if (status == ReportStatus.COMPLETED) AppColors.SuccessGreen else AppColors.WarningYellow
    Box(Modifier.background(color.copy(0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(if (isSpanish) (if (status == ReportStatus.COMPLETED) "Completado" else "En Progreso") else status.name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

// ==================== FEATURE: PROJECT MANAGEMENT ====================
@Composable
fun ProjectManagementView(language: String, onLanguageChange: (String) -> Unit, translation: String) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (language == "es") "Español" else "English", color = AppColors.TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.width(12.dp)); Switch(checked = language == "es", onCheckedChange = { onLanguageChange(if (it) "es" else "en") }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Accent, uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE0E0E0)))
            Spacer(Modifier.width(12.dp)); Text(translation, color = AppColors.TextSecondary, fontSize = 14.sp)
        }
        Text("Monthly Sales Revenue", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary, modifier = Modifier.padding(bottom = 4.dp))
        Text("Tracking revenue and losses based on category reports", color = AppColors.TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 24.dp))
        Card(modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 24.dp), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Gray50)) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val heights = listOf(80, 112, 64, 160, 128, 96, 144, 176, 144, 112, 128, 160)
                months.forEachIndexed { index, month -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.width(14.dp).height(heights[index].dp).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(AppColors.Accent)); Text(month, fontSize = 10.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(top = 8.dp)) } }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { MetadataItem("Location:", "New York"); MetadataItem("Initiative:", "Market Expansion"); MetadataItem("Date Range:", "2023-10-01 - 2023-12-31")
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Status: ", color = AppColors.TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp); Box(Modifier.clip(CircleShape).background(AppColors.Blue100).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("Not Started", color = AppColors.Blue800, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
        }
        Text("This chart automatically updates when new entries are added in the categories section", fontSize = 13.sp, color = AppColors.TextSecondary, style = androidx.compose.ui.text.TextStyle(fontStyle = FontStyle.Italic), modifier = Modifier.padding(top = 32.dp))
    }
}

@Composable
fun MetadataItem(label: String, value: String) { Row { Text(label, color = AppColors.TextSecondary, fontWeight = FontWeight.Medium, fontSize = 14.sp); Spacer(Modifier.width(4.dp)); Text(value, color = AppColors.TextPrimary, fontSize = 14.sp) } }

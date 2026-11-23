package com.example.rippleeffect

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
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
enum class Currency(val code: String, val symbol: String, @StringRes val nameRes: Int) {
    USD("USD", "$", R.string.currency_usd),
    GTQ("GTQ", "Q", R.string.currency_gtq),
    HNL("HNL", "L", R.string.currency_hnl)
}
enum class AppLifecycle { INITIALIZING, READY, ERROR }

// ==================== NEW DROPDOWN LANGUAGE SELECTOR ====================
// languages the app supports; tag must match the values-xx folder
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    SWAHILI("sw", "Kiswahili");

    companion object {
        fun fromTag(tag: String) = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

// ==================== DATA TABLE MODELS ====================
data class SubmittedEntry(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: Double,
    val currency: Currency,
    val type: String,
    val dateCreated: Date = Date()
)

enum class FilterField(@StringRes val labelRes: Int) {
    DESCRIPTION(R.string.description),
    AMOUNT(R.string.amount),
    CURRENCY(R.string.currency),
    TYPE(R.string.type),
    DATE(R.string.date)
}

enum class FilterOperator(@StringRes val labelRes: Int) {
    CONTAINS(R.string.op_contains),
    EQUALS(R.string.op_equals),
    GREATER_THAN(R.string.op_greater),
    LESS_THAN(R.string.op_less),
    BEFORE(R.string.op_before),
    AFTER(R.string.op_after)
}

enum class FilterConnector { AND, OR }

data class FilterCondition(
    val id: String = UUID.randomUUID().toString(),
    val field: FilterField = FilterField.DESCRIPTION,
    val operator: FilterOperator = FilterOperator.CONTAINS,
    val value: String = "",
    val connector: FilterConnector = FilterConnector.AND
)

enum class SortDirection { ASCENDING, DESCENDING }

data class SortConfig(
    val field: FilterField = FilterField.DATE,
    val direction: SortDirection = SortDirection.DESCENDING
)

// Result sealed class for email sending feedback
sealed class EmailResult {
    object Success : EmailResult()
    data class Failure(val message: String) : EmailResult()
}

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

// makes stringResource() inside content use the chosen language instead of the phone's
@Composable
fun ProvideAppLanguage(languageTag: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val localizedConfig = remember(languageTag, baseConfig) {
        Configuration(baseConfig).apply { setLocale(Locale.forLanguageTag(languageTag)) }
    }
    val localizedContext = remember(localizedConfig) {
        baseContext.createConfigurationContext(localizedConfig)
    }
    CompositionLocalProvider(
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext,
        content = content
    )
}

@Composable
fun RippleEffectApp() {
    var lifecycleState by remember { mutableStateOf(AppLifecycle.INITIALIZING) }
    var loadVersion by remember { mutableIntStateOf(0) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var language by remember { mutableStateOf("en") }
    val accountingEntries = remember { mutableStateListOf<AccountingEntry>() }
    val savedReports = remember { mutableStateListOf(
        SavedReport(name = "Annual Audit", dateCreated = Date(), status = ReportStatus.COMPLETED, owner = "John Doe", content = "Initial report content..."),
        SavedReport(name = "Q3 Summary", dateCreated = Date(System.currentTimeMillis() - 86400000), status = ReportStatus.IN_PROGRESS, owner = "Jane Smith", content = "Quarterly results...")
    ) }
    val submittedEntries = remember { mutableStateListOf<SubmittedEntry>() }

    LaunchedEffect(loadVersion) {
        lifecycleState = AppLifecycle.INITIALIZING
        runCatching {
            delay(500)
            val remote = Backend.fetchSubmittedEntries()
            if (remote.isNotEmpty()) {
                submittedEntries.clear()
                submittedEntries.addAll(remote.sortedByDescending { it.dateCreated })
            }
            lifecycleState = AppLifecycle.READY
        }.onFailure {
            Log.e(TAG, "Failed to initialize backend", it)
            lifecycleState = AppLifecycle.ERROR
        }
    }

    ProvideAppLanguage(language) {
        Crossfade(targetState = lifecycleState, label = "AppTransition") { state ->
            when (state) {
                AppLifecycle.INITIALIZING -> LoadingScreen()
                AppLifecycle.ERROR -> RetryScreen { loadVersion += 1 }
                AppLifecycle.READY -> MainContent(
                    selectedTab = selectedTab, onTabSelected = { selectedTab = it },
                    language = language, onLanguageChange = { language = it },
                    entries = accountingEntries, reports = savedReports,
                    submittedEntries = submittedEntries
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() { Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary, strokeWidth = 2.dp) } }

@Composable
fun RetryScreen(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().background(AppColors.DarkBackground), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.network_error), color = Color(0xFFD0D0D0), fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AppColors.RetryBlue)) { Text(stringResource(R.string.retry), fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    entries: MutableList<AccountingEntry>,
    reports: MutableList<SavedReport>,
    submittedEntries: MutableList<SubmittedEntry>
) {
    val scope = rememberCoroutineScope()
    var reportToEmail by remember { mutableStateOf<SavedReport?>(null) }
    var entriesToEmail by remember { mutableStateOf<List<AccountingEntry>?>(null) }
    var currentCurrency by remember { mutableStateOf(Currency.USD) }
    // null = no dialog; true = success; false = failure
    var emailResultDialogState by remember { mutableStateOf<EmailResult?>(null) }
    var tableFilters by remember { mutableStateOf(listOf<FilterCondition>()) }
    var tableSortConfig by remember { mutableStateOf(SortConfig()) }
    val context = LocalContext.current

    // ── Email result feedback dialog ──────────────────────────────────────────
    // ── Email result feedback dialog ──────────────────────────────────────────
    emailResultDialogState?.let { result ->
        AlertDialog(
            onDismissRequest = { emailResultDialogState = null },
            icon = {
                Icon(
                    imageVector = if (result is EmailResult.Success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result is EmailResult.Success) AppColors.SuccessGreen else AppColors.ErrorRed
                )
            },
            title = {
                Text(
                    if (result is EmailResult.Success) stringResource(R.string.email_draft_created)
                    else stringResource(R.string.email_open_failed)
                )
            },
            text = {
                Text(
                    when (result) {
                        is EmailResult.Success -> stringResource(R.string.email_draft_message)
                        is EmailResult.Failure -> stringResource(R.string.email_prepare_failed, result.message)
                    }
                )
            },
            confirmButton = {
                Button(onClick = { emailResultDialogState = null }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }

    // ── Email address input dialog ────────────────────────────────────────────
    if (reportToEmail != null) {
        var emailInput by remember { mutableStateOf("") }
        var emailInvalid by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { reportToEmail = null; entriesToEmail = null },
            title = { Text(stringResource(R.string.send_via_email)) },
            text = {
                Column {
                    Text(stringResource(R.string.enter_email))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it; emailInvalid = false },
                        label = { Text(stringResource(R.string.email_label)) },
                        isError = emailInvalid,
                        supportingText = if (emailInvalid) {
                            { Text(stringResource(R.string.invalid_email), color = AppColors.ErrorRed) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Basic email validation
                    val trimmed = emailInput.trim()
                    if (trimmed.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
                        emailInvalid = true
                        return@Button
                    }
                    reportToEmail?.let { report ->
                        val result = sendEmailWithReport(
                            context = context,
                            email = trimmed,
                            report = report,
                            entries = entriesToEmail ?: emptyList(),
                            currency = currentCurrency
                        )
                        emailResultDialogState = result
                    }
                    reportToEmail = null
                    entriesToEmail = null
                }) {
                    Text(stringResource(R.string.send))
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToEmail = null; entriesToEmail = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ripple_logo), // Make sure this matches your file name
                            contentDescription = stringResource(R.string.logo_description),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = { Box(Modifier.padding(end = 16.dp).size(32.dp).clip(CircleShape).background(AppColors.UserIconBg), contentAlignment = Alignment.Center) { Text("e", color = AppColors.UserIconText, fontWeight = FontWeight.Bold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AppColors.Surface, tonalElevation = 0.dp) {
                NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text(stringResource(R.string.report)) }, selected = selectedTab == 0, onClick = { onTabSelected(0) })
                NavigationBarItem(icon = { Icon(Icons.Default.Folder, null) }, label = { Text(stringResource(R.string.tab_categories)) }, selected = selectedTab == 1, onClick = { onTabSelected(1) })
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(AppColors.Background)) {
            when (selectedTab) {
                0 -> WaterSalesAccountingScreen(
                    language = language,
                    onLanguageChange = onLanguageChange,
                    entries = entries,
                    onReportSubmit = { newReport, currentEntries, currency ->
                        reports.add(0, newReport)
                        scope.launch {
                            Backend.saveSubmittedEntries(currentEntries, currency)
                        }
                        currentCurrency = currency
                        entriesToEmail = currentEntries
                        reportToEmail = newReport
                        currentEntries.forEach { entry ->
                            submittedEntries.add(0, SubmittedEntry(
                                description = entry.type,
                                amount = entry.amount,
                                currency = currency,
                                type = if (entry.isExpense) "Expense" else "Sale"
                            ))
                        }
                        onTabSelected(1)
                    }
                )
                1 -> DataTableScreen(
                    submittedEntries = submittedEntries,
                    filters = tableFilters,
                    onFiltersChanged = { tableFilters = it },
                    sortConfig = tableSortConfig,
                    onSortConfigChanged = { tableSortConfig = it },
                    onEmailRequest = { report, emailEntries, currency ->
                        currentCurrency = currency
                        entriesToEmail = emailEntries
                        reportToEmail = report
                    }
                )
            }
        }
    }
}

// ==================== FEATURE: WATER SALES ACCOUNTING (ITEMS TAB) ====================
@Composable
fun WaterSalesAccountingScreen(
    language: String,
    onLanguageChange: (String) -> Unit,
    entries: MutableList<AccountingEntry>,
    onReportSubmit: (SavedReport, List<AccountingEntry>, Currency) -> Unit
) {
    val context = LocalContext.current
    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    val cost = entries.filter { it.isExpense }.sumOf { it.amount }
    val income = entries.filter { !it.isExpense }.sumOf { it.amount }
    var summaryReport by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf("") }
    var entryAmount by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                var menuOpen by remember { mutableStateOf(false) }
                val current = AppLanguage.fromTag(language)
                Box {
                    OutlinedButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.Language, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(current.nativeName)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        AppLanguage.entries.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.nativeName) },
                                onClick = { onLanguageChange(lang.tag); menuOpen = false },
                                leadingIcon = if (lang == current) {
                                    { Icon(Icons.Default.Check, null, tint = AppColors.Primary) }
                                } else null
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.greeting), color = AppColors.TextSecondary, fontSize = 14.sp)
            }
        }
        item { Text(stringResource(R.string.water_sales_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountingSummaryCard(Modifier.weight(1f), stringResource(R.string.costs), cost, selectedCurrency, isExpense = true, Color(0xFFFEE2E2), Color(0xFF991B1B))
                AccountingSummaryCard(Modifier.weight(1f), stringResource(R.string.profit), income, selectedCurrency, isExpense = false, Color(0xFFDCFCE7), Color(0xFF166534))
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.select_currency), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Currency.values().forEach { curr ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedCurrency = curr }.padding(vertical = 4.dp)) {
                            RadioButton(selected = selectedCurrency == curr, onClick = { selectedCurrency = curr })
                            Text("${curr.symbol} ${curr.code} (${stringResource(curr.nameRes)})", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.data_entry), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = entryType, onValueChange = { entryType = it }, label = { Text(stringResource(R.string.description)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = entryAmount, onValueChange = { entryAmount = it }, label = { Text(stringResource(R.string.amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (entryType.isNotBlank()) { entries.add(AccountingEntry(type = entryType, amount = entryAmount.toDoubleOrNull() ?: 0.0, isExpense = true)); entryType = ""; entryAmount = "" } }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.ErrorRed), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_expense)) }
                        Button(onClick = { if (entryType.isNotBlank()) { entries.add(AccountingEntry(type = entryType, amount = entryAmount.toDoubleOrNull() ?: 0.0, isExpense = false)); entryType = ""; entryAmount = "" } }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.SuccessGreen), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.add_sale)) }
                    }
                    if (entries.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(stringResource(R.string.recent_records), fontWeight = FontWeight.Bold)
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
                        Text(stringResource(R.string.report), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        OutlinedButton(onClick = {}) { Icon(Icons.Default.Print, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.print)) }
                    }
                    Spacer(Modifier.height(16.dp))
                    Column(Modifier.fillMaxWidth().background(AppColors.CardBackground, RoundedCornerShape(8.dp)).padding(16.dp)) {
                        ReportRow(stringResource(R.string.total_costs_label), "${selectedCurrency.symbol}${String.format(Locale.US, "%.2f", cost)}")
                        ReportRow(stringResource(R.string.total_profit_label), "${selectedCurrency.symbol}${String.format(Locale.US, "%.2f", income)}")
                        ReportRow(stringResource(R.string.net_benefit_label), "${selectedCurrency.symbol}${String.format(Locale.US, "%.2f", income - cost)}")
                        if (entries.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp)); Text(stringResource(R.string.breakdown), fontWeight = FontWeight.Bold)
                            entries.forEach { entry -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(entry.type, fontSize = 12.sp, color = AppColors.TextSecondary); Text("${if (entry.isExpense) "-" else "+"}${selectedCurrency.symbol}${entry.amount}", fontSize = 12.sp, color = if (entry.isExpense) AppColors.ErrorRed else AppColors.SuccessGreen) } }
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.summary_optional), fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = summaryReport, onValueChange = { summaryReport = it }, placeholder = { Text(stringResource(R.string.summary_placeholder)) }, modifier = Modifier.fillMaxWidth().height(100.dp).padding(top = 8.dp))
                }
            }
        }
        item {
            Button(
                onClick = {
                    // onClick isn't @Composable, so use context.getString instead of stringResource
                    fun money(value: Double) = "${selectedCurrency.symbol}${String.format(Locale.US, "%.2f", value)}"
                    val reportContent = buildString {
                        append(context.getString(R.string.report_content_header)).append("\n")
                        append(context.getString(R.string.report_total_costs, money(cost))).append("\n")
                        append(context.getString(R.string.report_total_profit, money(income))).append("\n")
                        append(context.getString(R.string.report_net, money(income - cost))).append("\n\n")
                        append(context.getString(R.string.report_breakdown)).append("\n")
                        entries.forEach { append("- ${it.type}: ${if (it.isExpense) "-" else "+"}${selectedCurrency.symbol}${it.amount}\n") }
                        append("\n").append(context.getString(R.string.report_user_summary)).append("\n").append(summaryReport)
                    }
                    val datePart = SimpleDateFormat(context.getString(R.string.report_date_pattern), Locale.US).format(Date())
                    val newReport = SavedReport(
                        name = context.getString(R.string.report_name, datePart),
                        dateCreated = Date(),
                        status = ReportStatus.COMPLETED,
                        owner = "Current User",
                        content = reportContent
                    )
                    onReportSubmit(newReport, entries.toList(), selectedCurrency)
                    entries.clear(); summaryReport = ""
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary),
                shape = RoundedCornerShape(8.dp)
            ) { Text(stringResource(R.string.submit_report)) }
        }
    }
}

/**
 * Sends an email with the report as an attachment.
 *
 * FIX NOTES:
 * 1. `type` is now set BEFORE extras — some Android/email apps drop EXTRA_STREAM if type is set after.
 * 2. ClipData is attached for ACTION_SEND_MULTIPLE so URI read permissions propagate correctly.
 * 3. Returns an [EmailResult] so the caller can show success/failure UI.
 */
fun sendEmailWithReport(
    context: Context,
    email: String,
    report: SavedReport,
    entries: List<AccountingEntry>,
    currency: Currency
): EmailResult {
    return try {
        val cachePath = File(context.cacheDir, "reports")
        if (!cachePath.exists()) cachePath.mkdirs()

        val sanitizedFileName = report.name.replace(Regex("[^a-zA-Z0-9.-]"), "-")

        val txtFile = File(cachePath, "$sanitizedFileName.txt")
        txtFile.writeText(report.content)

        val uris = arrayListOf<Uri>()
        val txtUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", txtFile)
        uris.add(txtUri)

        if (entries.isNotEmpty()) {
            val csvContent = buildString {
                append("Description,Amount,Currency,Type\n")
                entries.forEach {
                    append("${it.type},${it.amount},${currency.code},${if (it.isExpense) "Expense" else "Sale"}\n")
                }
            }
            val csvFile = File(cachePath, "$sanitizedFileName.csv")
            csvFile.writeText(csvContent)
            val csvUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
            uris.add(csvUri)
        }

        // Build ClipData with ALL attachment URIs — required on Android 13+ for
        // FLAG_GRANT_READ_URI_PERMISSION to actually take effect.
        val clipData = ClipData.newUri(context.contentResolver, "attachment", uris[0]).also { cd ->
            uris.drop(1).forEach { uri -> cd.addItem(ClipData.Item(uri)) }
        }

        val intent: Intent
        if (uris.size > 1) {
            intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                setClipData(clipData)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                setClipData(clipData)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        intent.apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.email_subject, report.name))
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.email_body, report.name))
        }

        val chooser = Intent.createChooser(intent, context.getString(R.string.email_chooser_title))
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // Also grant URI permission explicitly to every package the chooser resolves
        val flags = android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        context.packageManager.queryIntentActivities(chooser, flags).forEach { info ->
            uris.forEach { uri ->
                context.grantUriPermission(info.activityInfo.packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        context.startActivity(chooser)
        EmailResult.Success

    } catch (e: Exception) {
        Log.e(TAG, "Error sending email", e)
        EmailResult.Failure(e.localizedMessage ?: "Unknown error. Ensure FileProvider is configured.")
    }
}

@Composable
fun AccountingSummaryCard(modifier: Modifier, title: String, amount: Double, currency: Currency, isExpense: Boolean, bgColor: Color, textColor: Color) {
    Card(modifier = modifier, border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 14.sp, color = AppColors.TextSecondary)
            Text("${currency.symbol}${String.format(Locale.US, "%.2f", amount)}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.background(bgColor, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(stringResource(if (isExpense) R.string.badge_expenses else R.string.badge_income), fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
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
fun CategoriesWithReportsScreen(reports: MutableList<SavedReport>, onReportCompleted: (SavedReport) -> Unit) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf<SavedReport?>(null) }
    var reportToDelete by remember { mutableStateOf<SavedReport?>(null) }

    if (selectedReport != null) {
        ReportEditorView(report = selectedReport!!,
            onExit = { if (selectedReport!!.status != ReportStatus.COMPLETED) selectedReport!!.status = ReportStatus.IN_PROGRESS; selectedReport = null },
            onComplete = {
                selectedReport!!.status = ReportStatus.COMPLETED
                onReportCompleted(selectedReport!!)
                selectedReport = null
            }
        )
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = { val newReport = SavedReport(name = context.getString(R.string.untitled_report), dateCreated = Date(), status = ReportStatus.IN_PROGRESS, owner = "Current User"); reports.add(0, newReport); selectedReport = newReport },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.add_new_report)) }
            Text(stringResource(R.string.saved_reports), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) { items(reports) { report -> ReportCardItem(report = report, onClick = { selectedReport = report }, onLongClick = { reportToDelete = report }) } }
        }
    }
    if (reportToDelete != null) {
        AlertDialog(onDismissRequest = { reportToDelete = null }, title = { Text(stringResource(R.string.delete_report)) },
            text = { Text(stringResource(R.string.delete_report_warning)) },
            confirmButton = { TextButton(onClick = { reports.remove(reportToDelete); reportToDelete = null }) { Text(stringResource(R.string.delete_caps), color = AppColors.ErrorRed) } },
            dismissButton = { TextButton(onClick = { reportToDelete = null }) { Text(stringResource(R.string.cancel_caps)) } }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportCardItem(report: SavedReport, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick), border = BorderStroke(1.dp, AppColors.Border), colors = CardDefaults.cardColors(containerColor = AppColors.Surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(report.name, fontWeight = FontWeight.Bold); Text(report.owner, fontSize = 12.sp, color = AppColors.TextSecondary) }
            StatusBadge(report.status)
        }
    }
}

@Composable
fun ReportEditorView(report: SavedReport, onExit: () -> Unit, onComplete: () -> Unit) {
    var isEditMode by remember { mutableStateOf(report.status == ReportStatus.IN_PROGRESS && report.content.isEmpty()) }
    var reportName by remember { mutableStateOf(report.name) }
    var reportContent by remember { mutableStateOf(report.content) }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    BackHandler { onExit() }
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        Surface(modifier = Modifier.fillMaxWidth(), color = if (isEditMode) AppColors.Blue100 else AppColors.Gray50, shadowElevation = 2.dp) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(if (isEditMode) R.string.editing_enabled else R.string.enable_editing), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* Print logic */ }) { Icon(Icons.Default.Print, null, tint = AppColors.TextPrimary) }
                    Spacer(Modifier.width(8.dp))
                    if (!isEditMode) { Button(onClick = { isEditMode = true }, colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)) { Text(stringResource(R.string.edit)) } }
                    else { TextButton(onClick = { isEditMode = false }) { Text(stringResource(R.string.view)) } }
                }
            }
        }
        AnimatedVisibility(visible = isEditMode) {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(AppColors.Gray50).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { isBold = !isBold }) { Icon(Icons.Default.FormatBold, null, tint = if (isBold) AppColors.Primary else AppColors.TextPrimary) }
                IconButton(onClick = { isItalic = !isItalic }) { Icon(Icons.Default.FormatItalic, null, tint = if (isItalic) AppColors.Primary else AppColors.TextPrimary) }
                IconButton(onClick = { isUnderline = !isUnderline }) { Icon(Icons.Default.FormatUnderlined, null, tint = if (isUnderline) AppColors.Primary else AppColors.TextPrimary) }
                IconButton(onClick = { fontSize += 2f }) { Icon(Icons.Default.TextIncrease, null) }
                IconButton(onClick = { if (fontSize > 8f) fontSize -= 2f }) { Icon(Icons.Default.TextDecrease, null) }
                IconButton(onClick = { }) { Icon(Icons.Default.GridOn, null) }
                IconButton(onClick = { }) { Icon(Icons.Default.Image, null) }
            }
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp)) {
            if (isEditMode) {
                OutlinedTextField(value = reportName, onValueChange = { reportName = it; report.name = it }, label = { Text(stringResource(R.string.report_title_label)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = reportContent, onValueChange = { reportContent = it; report.content = it }, placeholder = { Text(stringResource(R.string.report_body_placeholder)) }, modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = fontSize.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal, textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None))
            } else { Text(reportName, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(16.dp)); Text(reportContent, fontSize = fontSize.sp) }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.back)) }
            Button(onClick = onComplete, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppColors.SuccessGreen)) { Text(stringResource(R.string.complete)) }
        }
    }
}

@Composable
fun StatusBadge(status: ReportStatus) {
    val color = if (status == ReportStatus.COMPLETED) AppColors.SuccessGreen else AppColors.WarningYellow
    val label = when (status) {
        ReportStatus.COMPLETED -> R.string.status_completed
        ReportStatus.IN_PROGRESS -> R.string.status_in_progress
        ReportStatus.DRAFT -> R.string.status_draft
    }
    Box(Modifier.background(color.copy(0.1f), RoundedCornerShape(16.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(stringResource(label), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

// stored type stays "Expense"/"Sale"; this is only for showing it on screen
@Composable
fun typeLabel(type: String): String =
    stringResource(if (type == "Expense") R.string.type_expense else R.string.type_sale)

// ==================== FEATURE: DATA TABLE (REPORTS TAB) ====================

fun getOperatorsForField(field: FilterField): List<FilterOperator> = when (field) {
    FilterField.DESCRIPTION -> listOf(FilterOperator.CONTAINS, FilterOperator.EQUALS)
    FilterField.AMOUNT -> listOf(FilterOperator.EQUALS, FilterOperator.GREATER_THAN, FilterOperator.LESS_THAN)
    FilterField.CURRENCY -> listOf(FilterOperator.EQUALS)
    FilterField.TYPE -> listOf(FilterOperator.EQUALS)
    FilterField.DATE -> listOf(FilterOperator.EQUALS, FilterOperator.BEFORE, FilterOperator.AFTER)
}

fun evaluateCondition(entry: SubmittedEntry, condition: FilterCondition): Boolean {
    if (condition.value.isBlank()) return true
    val fv = condition.value.trim().lowercase()
    return when (condition.field) {
        FilterField.DESCRIPTION -> when (condition.operator) {
            FilterOperator.CONTAINS -> entry.description.lowercase().contains(fv)
            FilterOperator.EQUALS -> entry.description.lowercase() == fv
            else -> true
        }
        FilterField.AMOUNT -> {
            val nv = fv.toDoubleOrNull() ?: return true
            when (condition.operator) {
                FilterOperator.EQUALS -> entry.amount == nv
                FilterOperator.GREATER_THAN -> entry.amount > nv
                FilterOperator.LESS_THAN -> entry.amount < nv
                else -> true
            }
        }
        FilterField.CURRENCY -> entry.currency.code.lowercase() == fv
        FilterField.TYPE -> entry.type.lowercase() == fv
        FilterField.DATE -> {
            val fmt = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val filterDate = try { fmt.parse(condition.value.trim()) } catch (_: Exception) { null } ?: return true
            val entryDayStr = fmt.format(entry.dateCreated)
            val entryDay = try { fmt.parse(entryDayStr) } catch (_: Exception) { null } ?: return true
            when (condition.operator) {
                FilterOperator.EQUALS -> entryDayStr == fmt.format(filterDate)
                FilterOperator.BEFORE -> entryDay.before(filterDate)
                FilterOperator.AFTER -> entryDay.after(filterDate)
                else -> true
            }
        }
    }
}

fun applyFilters(entries: List<SubmittedEntry>, filters: List<FilterCondition>): List<SubmittedEntry> {
    val active = filters.filter { it.value.isNotBlank() }
    if (active.isEmpty()) return entries
    return entries.filter { entry ->
        var result = evaluateCondition(entry, active[0])
        for (i in 1 until active.size) {
            val next = evaluateCondition(entry, active[i])
            result = when (active[i].connector) {
                FilterConnector.AND -> result && next
                FilterConnector.OR -> result || next
            }
        }
        result
    }
}

fun applySorting(entries: List<SubmittedEntry>, cfg: SortConfig): List<SubmittedEntry> {
    val cmp: Comparator<SubmittedEntry> = when (cfg.field) {
        FilterField.DESCRIPTION -> compareBy { it.description.lowercase() }
        FilterField.AMOUNT -> compareBy { it.amount }
        FilterField.CURRENCY -> compareBy { it.currency.code }
        FilterField.TYPE -> compareBy { it.type }
        FilterField.DATE -> compareBy { it.dateCreated }
    }
    return if (cfg.direction == SortDirection.ASCENDING) entries.sortedWith(cmp) else entries.sortedWith(cmp.reversed())
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DataTableScreen(
    submittedEntries: List<SubmittedEntry>,
    filters: List<FilterCondition>,
    onFiltersChanged: (List<FilterCondition>) -> Unit,
    sortConfig: SortConfig,
    onSortConfigChanged: (SortConfig) -> Unit,
    onEmailRequest: (SavedReport, List<AccountingEntry>, Currency) -> Unit
) {
    val context = LocalContext.current
    var showFilters by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US) }
    val filteredEntries = applyFilters(submittedEntries, filters)
    val sortedEntries = applySorting(filteredEntries, sortConfig)
    val horizontalScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Title row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.data_table_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showFilters = !showFilters }) {
                    Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.filter))
                    if (filters.any { it.value.isNotBlank() }) {
                        Spacer(Modifier.width(4.dp))
                        Box(Modifier.size(20.dp).background(AppColors.Primary, CircleShape), contentAlignment = Alignment.Center) {
                            Text("${filters.count { it.value.isNotBlank() }}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (sortedEntries.isNotEmpty()) {
                            val reportContent = buildString {
                                append(context.getString(R.string.export_header)).append("\n")
                                append(context.getString(R.string.export_generated, dateFormat.format(Date()))).append("\n")
                                append(context.getString(R.string.export_records, sortedEntries.size)).append("\n\n")
                                sortedEntries.forEach { e ->
                                    append("${e.description} | ${e.currency.symbol}${String.format(Locale.US, "%.2f", e.amount)} | ${e.currency.code} | ${e.type} | ${dateFormat.format(e.dateCreated)}\n")
                                }
                            }
                            val datePart = SimpleDateFormat(context.getString(R.string.report_date_pattern), Locale.US).format(Date())
                            val report = SavedReport(
                                name = context.getString(R.string.export_name, datePart),
                                dateCreated = Date(), status = ReportStatus.COMPLETED, owner = "Current User", content = reportContent
                            )
                            val acctEntries = sortedEntries.map { AccountingEntry(type = it.description, amount = it.amount, isExpense = it.type == "Expense") }
                            val currency = sortedEntries.firstOrNull()?.currency ?: Currency.USD
                            onEmailRequest(report, acctEntries, currency)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(Icons.Default.Email, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.email_button))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Filters
        AnimatedVisibility(visible = showFilters) {
            FilterSection(filters = filters, onFiltersChanged = onFiltersChanged)
        }

        // Record count
        Text(
            pluralStringResource(R.plurals.record_count, sortedEntries.size, sortedEntries.size),
            fontSize = 12.sp, color = AppColors.TextSecondary, modifier = Modifier.padding(vertical = 8.dp)
        )

        if (submittedEntries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GridOn, null, Modifier.size(48.dp), tint = AppColors.TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.no_data_yet), color = AppColors.TextSecondary, fontSize = 16.sp)
                    Text(stringResource(R.string.no_data_hint), color = AppColors.TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                border = BorderStroke(1.dp, AppColors.Border),
                colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
            ) {
                Column {
                    // Table header
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)
                            .background(AppColors.Gray50).padding(vertical = 6.dp)
                    ) {
                        SortableColumnHeader(stringResource(R.string.description), FilterField.DESCRIPTION, sortConfig, onSortConfigChanged, 140.dp)
                        SortableColumnHeader(stringResource(R.string.amount), FilterField.AMOUNT, sortConfig, onSortConfigChanged, 100.dp)
                        SortableColumnHeader(stringResource(R.string.currency), FilterField.CURRENCY, sortConfig, onSortConfigChanged, 85.dp)
                        SortableColumnHeader(stringResource(R.string.type), FilterField.TYPE, sortConfig, onSortConfigChanged, 85.dp)
                        SortableColumnHeader(stringResource(R.string.date), FilterField.DATE, sortConfig, onSortConfigChanged, 130.dp)
                    }
                    HorizontalDivider(color = AppColors.Border, thickness = 1.dp)

                    // Table body
                    if (sortedEntries.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.no_filter_results),
                                color = AppColors.TextSecondary, fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxWidth()) {
                            items(sortedEntries, key = { it.id }) { entry ->
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(horizontalScrollState)
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(entry.description, Modifier.width(140.dp).padding(horizontal = 12.dp), fontSize = 13.sp, maxLines = 1)
                                    Text(
                                        "${entry.currency.symbol}${String.format(Locale.US, "%.2f", entry.amount)}",
                                        Modifier.width(100.dp).padding(horizontal = 12.dp), fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (entry.type == "Expense") AppColors.ErrorRed else AppColors.SuccessGreen
                                    )
                                    Text(entry.currency.code, Modifier.width(85.dp).padding(horizontal = 12.dp), fontSize = 13.sp)
                                    Box(Modifier.width(85.dp).padding(horizontal = 12.dp)) {
                                        val tc = if (entry.type == "Expense") AppColors.ErrorRed else AppColors.SuccessGreen
                                        Box(Modifier.background(tc.copy(0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                                            Text(typeLabel(entry.type), fontSize = 11.sp, color = tc, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    Text(
                                        dateFormat.format(entry.dateCreated),
                                        Modifier.width(130.dp).padding(horizontal = 12.dp), fontSize = 13.sp,
                                        color = AppColors.TextSecondary
                                    )
                                }
                                HorizontalDivider(color = AppColors.Border)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortableColumnHeader(
    label: String, field: FilterField, sortConfig: SortConfig,
    onSortChanged: (SortConfig) -> Unit, width: Dp
) {
    var showMenu by remember { mutableStateOf(false) }
    val active = sortConfig.field == field
    Box(Modifier.width(width).padding(horizontal = 12.dp)) {
        Row(Modifier.clickable { showMenu = true }, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (active) AppColors.Primary else AppColors.TextPrimary, maxLines = 1)
            Spacer(Modifier.width(2.dp))
            Text(
                if (active) (if (sortConfig.direction == SortDirection.ASCENDING) "▲" else "▼") else "↕",
                fontSize = 10.sp, color = if (active) AppColors.Primary else AppColors.TextSecondary
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_ascending)) },
                onClick = { onSortChanged(SortConfig(field, SortDirection.ASCENDING)); showMenu = false },
                leadingIcon = if (active && sortConfig.direction == SortDirection.ASCENDING) {
                    { Icon(Icons.Default.Check, null, tint = AppColors.Primary) }
                } else null
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.sort_descending)) },
                onClick = { onSortChanged(SortConfig(field, SortDirection.DESCENDING)); showMenu = false },
                leadingIcon = if (active && sortConfig.direction == SortDirection.DESCENDING) {
                    { Icon(Icons.Default.Check, null, tint = AppColors.Primary) }
                } else null
            )
        }
    }
}

@Composable
fun FilterSection(filters: List<FilterCondition>, onFiltersChanged: (List<FilterCondition>) -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        border = BorderStroke(1.dp, AppColors.Border),
        colors = CardDefaults.cardColors(containerColor = AppColors.Gray50)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (filters.isEmpty()) {
                Button(
                    onClick = { onFiltersChanged(listOf(FilterCondition())) },
                    Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.add_filter))
                }
            } else {
                filters.forEachIndexed { index, condition ->
                    if (index > 0) {
                        // AND / OR connector toggle
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                            val isAnd = condition.connector == FilterConnector.AND
                            OutlinedButton(
                                onClick = {
                                    onFiltersChanged(filters.toMutableList().also { it[index] = condition.copy(connector = FilterConnector.AND) })
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isAnd) AppColors.Primary.copy(0.1f) else Color.Transparent
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(stringResource(R.string.filter_and), fontSize = 12.sp, fontWeight = if (isAnd) FontWeight.Bold else FontWeight.Normal, color = if (isAnd) AppColors.Primary else AppColors.TextSecondary)
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    onFiltersChanged(filters.toMutableList().also { it[index] = condition.copy(connector = FilterConnector.OR) })
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isAnd) AppColors.Primary.copy(0.1f) else Color.Transparent
                                ),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(stringResource(R.string.filter_or), fontSize = 12.sp, fontWeight = if (!isAnd) FontWeight.Bold else FontWeight.Normal, color = if (!isAnd) AppColors.Primary else AppColors.TextSecondary)
                            }
                        }
                    }
                    FilterConditionRow(
                        condition,
                        onChanged = { updated -> onFiltersChanged(filters.toMutableList().also { it[index] = updated }) },
                        onRemove = { onFiltersChanged(filters.toMutableList().also { it.removeAt(index) }) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onFiltersChanged(filters + FilterCondition()) }) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add), fontSize = 13.sp)
                    }
                    TextButton(onClick = { onFiltersChanged(emptyList()) }) {
                        Text(stringResource(R.string.clear_all), fontSize = 13.sp, color = AppColors.ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterConditionRow(
    condition: FilterCondition,
    onChanged: (FilterCondition) -> Unit,
    onRemove: () -> Unit
) {
    var fieldExpanded by remember { mutableStateOf(false) }
    var opExpanded by remember { mutableStateOf(false) }
    val ops = getOperatorsForField(condition.field)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Field selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { fieldExpanded = true },
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(stringResource(condition.field.labelRes), fontSize = 11.sp, maxLines = 1)
            }
            DropdownMenu(expanded = fieldExpanded, onDismissRequest = { fieldExpanded = false }) {
                FilterField.values().forEach { f ->
                    DropdownMenuItem(
                        text = { Text(stringResource(f.labelRes), fontSize = 13.sp) },
                        onClick = {
                            val newOps = getOperatorsForField(f)
                            val newOp = if (condition.operator in newOps) condition.operator else newOps.first()
                            onChanged(condition.copy(field = f, operator = newOp, value = ""))
                            fieldExpanded = false
                        }
                    )
                }
            }
        }
        // Operator selector
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { opExpanded = true },
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(stringResource(condition.operator.labelRes), fontSize = 11.sp, maxLines = 1)
            }
            DropdownMenu(expanded = opExpanded, onDismissRequest = { opExpanded = false }) {
                ops.forEach { op ->
                    DropdownMenuItem(
                        text = { Text(stringResource(op.labelRes), fontSize = 13.sp) },
                        onClick = { onChanged(condition.copy(operator = op)); opExpanded = false }
                    )
                }
            }
        }
        // Value input — context-aware
        Box(Modifier.weight(1.2f)) {
            when (condition.field) {
                FilterField.CURRENCY -> {
                    var exp by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { exp = true }, Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(condition.value.ifBlank { stringResource(R.string.choose) }, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        Currency.values().forEach { c ->
                            DropdownMenuItem(
                                text = { Text("${c.symbol} ${c.code}", fontSize = 13.sp) },
                                onClick = { onChanged(condition.copy(value = c.code)); exp = false }
                            )
                        }
                    }
                }
                FilterField.TYPE -> {
                    var exp by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = { exp = true }, Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(if (condition.value.isBlank()) stringResource(R.string.choose) else typeLabel(condition.value), fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = exp, onDismissRequest = { exp = false }) {
                        listOf("Expense" to R.string.type_expense, "Sale" to R.string.type_sale).forEach { (v, l) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(l), fontSize = 13.sp) },
                                onClick = { onChanged(condition.copy(value = v)); exp = false }
                            )
                        }
                    }
                }
                else -> {
                    OutlinedTextField(
                        value = condition.value,
                        onValueChange = { onChanged(condition.copy(value = it)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        placeholder = {
                            Text(
                                when (condition.field) {
                                    FilterField.DATE -> stringResource(R.string.date_input_pattern)
                                    FilterField.AMOUNT -> "0.00"
                                    else -> stringResource(R.string.search_placeholder)
                                },
                                fontSize = 11.sp
                            )
                        },
                        keyboardOptions = if (condition.field == FilterField.AMOUNT) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default
                    )
                }
            }
        }
        // Remove button
        IconButton(onClick = onRemove, Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = AppColors.ErrorRed)
        }
    }
}

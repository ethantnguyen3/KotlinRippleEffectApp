package com.example.rippleeffect

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx. compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose. foundation.text.KeyboardOptions
import androidx. compose.material. icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose. runtime.*
import androidx.compose.ui. Alignment
import androidx.compose.ui. Modifier
import androidx.compose.ui. draw.clip
import androidx.compose.ui. graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx. compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx. compose.ui.text.style.TextAlign
import androidx.compose.ui.unit. dp
import androidx. compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "RippleEffect"

// App Colors matching HTML/Manifest
object AppColors {
    val Primary = Color(0xFF613DC7)
    val Background = Color(0xFFF6F6F6)
    val Surface = Color. White
    val TextPrimary = Color(0xFF182026)
    val TextSecondary = Color(0xFF707070)
    val TextTertiary = Color(0xFFADADAD)
    val ErrorRed = Color(0xFFF0554C)
    val SuccessGreen = Color(0xFF10B97A)
    val WarningYellow = Color(0xFFFBBF24)
    val RetryBlue = Color(0xFF0077EE)
    val BorderColor = Color(0xFFEFEFEF)
    val CardBackground = Color(0xFFF5F5F5)
}

// Language enum for i18n
enum class Language(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español")
}

// Currency enum
enum class Currency(val code: String, val symbol: String, val nameEn: String, val nameEs: String) {
    USD("USD", "$", "US Dollar", "Dólar EEUU"),
    GTQ("GTQ", "Q", "Quetzal", "Quetzal"),
    HNL("HNL", "L", "Lempira", "Lempira")
}

// Data classes
data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val type: ExpenseType,
    val amount: Double,
    val date: Date = Date()
)

enum class ExpenseType {
    BOTTLES, BILLS, SUPPLIES, OTHER
}

data class Sale(
    val id: String = UUID. randomUUID().toString(),
    val type: SaleType,
    val amount:  Double,
    val date: Date = Date()
)

enum class SaleType {
    WATER_BY_BOTTLE, WATER_BY_GALLON, OTHER
}

data class Transaction(
    val id: String = UUID. randomUUID().toString(),
    val date: Date,
    val amount: Double,
    val description: String = ""
)

data class SavedReport(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dateCreated: Date,
    val status: ReportStatus,
    val owner: String
)

enum class ReportStatus {
    COMPLETED, IN_PROGRESS, DRAFT
}

data class MonthlyRevenue(
    val month:  String,
    val year: Int,
    val revenue: Double,
    val expenses: Double
)

enum class AppLifecycle {
    LOADING, LOADED, ERROR
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "✓ performance. now(): App Lifecycle Started")

        setContent {
            RippleEffectTheme {
                RippleEffectApp()
            }
        }
    }
}

@Composable
fun RippleEffectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Primary,
            background = AppColors.Background,
            surface = AppColors.Surface,
            onSurface = AppColors.TextPrimary,
            onBackground = AppColors.TextPrimary
        ),
        content = content
    )
}

@Composable
fun RippleEffectApp() {
    var lifecycleState by remember { mutableStateOf(AppLifecycle.LOADING) }
    var selectedTab by remember { mutableStateOf(0) }
    var language by remember { mutableStateOf(Language. ENGLISH) }

    LaunchedEffect(Unit) {
        Log.d(TAG, "✓ Initializing ServiceWorker simulation...")
        delay(2000)
        lifecycleState = AppLifecycle. LOADED
        Log. d(TAG, "✓ glidebeacon:  true - Resources Loaded")
    }

    Crossfade(targetState = lifecycleState, label = "AppTransition") { state ->
        when (state) {
            AppLifecycle. LOADING -> LoadingScreen()
            AppLifecycle. ERROR -> RetryScreen { lifecycleState = AppLifecycle. LOADING }
            AppLifecycle. LOADED -> MainContent(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                language = language,
                onLanguageChange = { language = it }
            )
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            . fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.Primary)
            Spacer(Modifier.height(16.dp))
            Text("Initializing.. .", color = AppColors.TextSecondary)
        }
    }
}

@Composable
fun RetryScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            . fillMaxSize()
            .background(Color(0xFF131315)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Could not load network resources",
                color = Color(0xFFD0D0D0),
                fontSize = 16.sp
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .width(100.dp)
                    .height(40.dp)
                    .border(2.dp, AppColors.RetryBlue, RoundedCornerShape(4.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color. White,
                    contentColor = AppColors.RetryBlue
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Retry", fontWeight = FontWeight. Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    language: Language,
    onLanguageChange:  (Language) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ripple Effect", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors. Surface)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AppColors. Surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text(if (language == Language.ENGLISH) "Items" else "Artículos") },
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons. Default. Folder, null) },
                    label = { Text(if (language == Language.ENGLISH) "Categories" else "Categorías") },
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons. Default.Assessment, null) },
                    label = { Text(if (language == Language.ENGLISH) "Project Mgmt" else "Gestión") },
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ItemsScreen(language = language, onLanguageChange = onLanguageChange)
                1 -> CategoriesScreen(language = language)
                2 -> ProjectManagementScreen(language = language)
            }
        }
    }
}

// ==================== ITEMS SCREEN (Water Sales Accounting) ====================

@Composable
fun ItemsScreen(
    language: Language,
    onLanguageChange: (Language) -> Unit
) {
    var cost by remember { mutableStateOf(15.99) }
    var profit by remember { mutableStateOf(5.0) }
    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var expenses by remember { mutableStateOf(listOf<Expense>()) }
    var sales by remember { mutableStateOf(listOf<Sale>()) }
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var isPrintable by remember { mutableStateOf(true) }

    // Form states
    var expenseType by remember { mutableStateOf(ExpenseType.BOTTLES) }
    var expenseAmount by remember { mutableStateOf("") }
    var saleType by remember { mutableStateOf(SaleType. WATER_BY_BOTTLE) }
    var saleAmount by remember { mutableStateOf("") }
    var transactionDate by remember { mutableStateOf("") }
    var transactionAmount by remember { mutableStateOf("") }

    val isEnglish = language == Language. ENGLISH

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = if (isEnglish) "Water Sales Accounting" else "Contabilidad de Ventas de Agua",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
        }

        // Language Toggle
        item {
            LanguageToggle(
                language = language,
                onLanguageChange = onLanguageChange
            )
        }

        // Summary Cards
        item {
            Row(
                modifier = Modifier. fillMaxWidth(),
                horizontalArrangement = Arrangement. spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "Costs" else "Costos",
                    amount = cost,
                    currency = selectedCurrency,
                    badgeText = if (isEnglish) "Expenses" else "Gastos",
                    badgeColor = Color(0xFFFEE2E2),
                    badgeTextColor = Color(0xFF991B1B)
                )
                SummaryCard(
                    modifier = Modifier. weight(1f),
                    title = if (isEnglish) "Profit" else "Ganancias",
                    amount = profit,
                    currency = selectedCurrency,
                    badgeText = if (isEnglish) "Income" else "Ingresos",
                    badgeColor = Color(0xFFDCFCE7),
                    badgeTextColor = Color(0xFF166534),
                    amountColor = AppColors. SuccessGreen
                )
            }
        }

        // Currency Selection
        item {
            CurrencySelectionCard(
                selectedCurrency = selectedCurrency,
                onCurrencySelected = { selectedCurrency = it },
                isEnglish = isEnglish
            )
        }

        // Data Entry Section
        item {
            DataEntryCard(
                isEnglish = isEnglish,
                expenseType = expenseType,
                onExpenseTypeChange = { expenseType = it },
                expenseAmount = expenseAmount,
                onExpenseAmountChange = { expenseAmount = it },
                onAddExpense = {
                    val amount = expenseAmount.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        expenses = expenses + Expense(type = expenseType, amount = amount)
                        cost += amount
                        expenseAmount = ""
                    }
                },
                saleType = saleType,
                onSaleTypeChange = { saleType = it },
                saleAmount = saleAmount,
                onSaleAmountChange = { saleAmount = it },
                onAddSale = {
                    val amount = saleAmount. toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        sales = sales + Sale(type = saleType, amount = amount)
                        profit += amount
                        saleAmount = ""
                    }
                }
            )
        }

        // Report Section
        item {
            ReportCard(
                isEnglish = isEnglish,
                isPrintable = isPrintable,
                cost = cost,
                profit = profit,
                currency = selectedCurrency,
                expenses = expenses
            )
        }

        // Transaction Entry
        item {
            TransactionEntryCard(
                isEnglish = isEnglish,
                date = transactionDate,
                onDateChange = { transactionDate = it },
                amount = transactionAmount,
                onAmountChange = { transactionAmount = it },
                onAddTransaction = {
                    val amount = transactionAmount.toDoubleOrNull()
                    if (amount != null && transactionDate. isNotBlank()) {
                        transactions = transactions + Transaction(
                            date = Date(),
                            amount = amount,
                            description = "Transaction on $transactionDate"
                        )
                        transactionDate = ""
                        transactionAmount = ""
                    }
                }
            )
        }

        // Summary display for transaction
        if (transactionDate.isNotBlank() && transactionAmount. isNotBlank()) {
            item {
                val amount = transactionAmount. toDoubleOrNull() ?: 0.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppColors. CardBackground),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isEnglish)
                            "Transaction of ${selectedCurrency.symbol}${String.format("%.2f", amount)} recorded for $transactionDate"
                        else
                            "Transacción de ${selectedCurrency.symbol}${String.format("%.2f", amount)} registrada para $transactionDate",
                        modifier = Modifier.padding(12.dp),
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageToggle(
    language: Language,
    onLanguageChange: (Language) -> Unit
) {
    Row(
        modifier = Modifier. fillMaxWidth(),
        horizontalArrangement = Arrangement. End
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, AppColors.BorderColor),
            colors = CardDefaults. cardColors(containerColor = Color. Transparent)
        ) {
            Row {
                Language. values().forEach { lang ->
                    val isSelected = language == lang
                    Box(
                        modifier = Modifier
                            .clickable { onLanguageChange(lang) }
                            .background(if (isSelected) AppColors.Primary else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = lang. displayName,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color. White else AppColors. TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    modifier:  Modifier = Modifier,
    title: String,
    amount: Double,
    currency: Currency,
    badgeText: String,
    badgeColor:  Color,
    badgeTextColor: Color,
    amountColor: Color = AppColors.TextPrimary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${currency.symbol}${String.format("%.2f", amount)}",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = badgeTextColor
                )
            }
        }
    }
}

@Composable
fun CurrencySelectionCard(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    isEnglish: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isEnglish) "Select Currency" else "Seleccionar Moneda",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier. height(16.dp))

            Currency.values().forEach { currency ->
                val isSelected = selectedCurrency == currency
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onCurrencySelected(currency) },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSelected) AppColors.Primary else AppColors.BorderColor),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) AppColors.Primary. copy(alpha = 0.1f) else AppColors.Surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onCurrencySelected(currency) },
                            colors = RadioButtonDefaults.colors(selectedColor = AppColors.Primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = currency.symbol,
                            fontWeight = FontWeight. Bold,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign. Center
                        )
                        Text(
                            text = "${currency.code} ",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "(${if (isEnglish) currency.nameEn else currency.nameEs})",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isEnglish)
                    "Note: All transactions will be recorded in the selected currency."
                else
                    "Nota:  Todas las transacciones se registrarán en la moneda seleccionada.",
                color = AppColors.TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataEntryCard(
    isEnglish: Boolean,
    expenseType:  ExpenseType,
    onExpenseTypeChange: (ExpenseType) -> Unit,
    expenseAmount: String,
    onExpenseAmountChange: (String) -> Unit,
    onAddExpense:  () -> Unit,
    saleType: SaleType,
    onSaleTypeChange: (SaleType) -> Unit,
    saleAmount: String,
    onSaleAmountChange: (String) -> Unit,
    onAddSale: () -> Unit
) {
    var expenseExpanded by remember { mutableStateOf(false) }
    var saleExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier. fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isEnglish) "Data Entry" else "Registro de Datos",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors. TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            // Add Expenses
            Text(
                text = if (isEnglish) "Add Expenses" else "Añadir Gastos",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Expense Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expenseExpanded,
                    onExpandedChange = { expenseExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = getExpenseTypeName(expenseType, isEnglish),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isEnglish) "Expense Type" else "Tipo de Gasto") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expenseExpanded,
                        onDismissRequest = { expenseExpanded = false }
                    ) {
                        ExpenseType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getExpenseTypeName(type, isEnglish)) },
                                onClick = {
                                    onExpenseTypeChange(type)
                                    expenseExpanded = false
                                }
                            )
                        }
                    }
                }

                // Expense Amount
                OutlinedTextField(
                    value = expenseAmount,
                    onValueChange = onExpenseAmountChange,
                    label = { Text(if (isEnglish) "Amount" else "Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType. Decimal),
                    modifier = Modifier. weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddExpense,
                colors = ButtonDefaults. buttonColors(containerColor = AppColors. Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEnglish) "Add Expense" else "Añadir Gasto")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Add Sales
            Text(
                text = if (isEnglish) "Add Sales" else "Añadir Ventas",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier. height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sale Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = saleExpanded,
                    onExpandedChange = { saleExpanded = it },
                    modifier = Modifier. weight(1f)
                ) {
                    OutlinedTextField(
                        value = getSaleTypeName(saleType, isEnglish),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isEnglish) "Sale Type" else "Tipo de Venta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = saleExpanded) },
                        modifier = Modifier. menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = saleExpanded,
                        onDismissRequest = { saleExpanded = false }
                    ) {
                        SaleType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(getSaleTypeName(type, isEnglish)) },
                                onClick = {
                                    onSaleTypeChange(type)
                                    saleExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sale Amount
                OutlinedTextField(
                    value = saleAmount,
                    onValueChange = onSaleAmountChange,
                    label = { Text(if (isEnglish) "Amount" else "Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddSale,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors. Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEnglish) "Add Sale" else "Añadir Venta")
            }
        }
    }
}

@Composable
fun ReportCard(
    isEnglish:  Boolean,
    isPrintable: Boolean,
    cost: Double,
    profit:  Double,
    currency: Currency,
    expenses: List<Expense>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Report" else "Informe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                OutlinedButton(
                    onClick = { /* Print action */ },
                    enabled = isPrintable,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isEnglish) "Print" else "Imprimir")
                }
            }

            Spacer(Modifier.height(16.dp))

            // Report Content
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults. cardColors(
                    containerColor = if (isPrintable) AppColors.CardBackground else AppColors.Surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEnglish) "Water Sales Report" else "Informe de Ventas de Agua",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors. TextPrimary,
                        textAlign = TextAlign. Center,
                        modifier = Modifier. fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    // Summary
                    Text(
                        text = if (isEnglish) "Summary" else "Resumen",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors. TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    ReportRow(
                        label = if (isEnglish) "Total Costs:" else "Costos Totales:",
                        value = "${currency.symbol}${String.format("%.2f", cost)}"
                    )
                    ReportRow(
                        label = if (isEnglish) "Total Profit:" else "Ganancias Totales:",
                        value = "${currency.symbol}${String.format("%.2f", profit)}"
                    )
                    ReportRow(
                        label = if (isEnglish) "Net Benefit:" else "Beneficio Neto:",
                        value = "${currency.symbol}${String.format("%.2f", profit - cost)}",
                        showDivider = false
                    )

                    Spacer(Modifier.height(16.dp))

                    // Expense Breakdown
                    Text(
                        text = if (isEnglish) "Expense Breakdown" else "Desglose de Gastos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))

                    // Static sample data matching HTML
                    ReportRow(
                        label = if (isEnglish) "Bottles" else "Botellas",
                        value = "${currency.symbol}45. 00"
                    )
                    ReportRow(
                        label = if (isEnglish) "Bills" else "Facturas",
                        value = "${currency. symbol}120.00"
                    )
                    ReportRow(
                        label = if (isEnglish) "Supplies" else "Suministros",
                        value = "${currency.symbol}35.00",
                        showDivider = false
                    )
                }
            }
        }
    }
}

@Composable
fun ReportRow(
    label: String,
    value: String,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = AppColors.TextSecondary)
            Text(text = value, fontWeight = FontWeight. SemiBold, color = AppColors.TextPrimary)
        }
        if (showDivider) {
            HorizontalDivider(color = AppColors. BorderColor)
        }
    }
}

@Composable
fun TransactionEntryCard(
    isEnglish:  Boolean,
    date: String,
    onDateChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    onAddTransaction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isEnglish) "Transaction Date" else "Fecha de Transacción",
                fontWeight = FontWeight. SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = date,
                onValueChange = onDateChange,
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (isEnglish) "Transaction Amount" else "Monto de Transacción",
                fontWeight = FontWeight. SemiBold,
                color = AppColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                placeholder = { Text("0.00") },
                leadingIcon = { Text("$", color = AppColors.TextSecondary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType. Decimal),
                modifier = Modifier. fillMaxWidth()
            )
        }
    }
}

// Helper functions
fun getExpenseTypeName(type: ExpenseType, isEnglish:  Boolean): String {
    return when (type) {
        ExpenseType. BOTTLES -> if (isEnglish) "Bottles" else "Botellas"
        ExpenseType. BILLS -> if (isEnglish) "Bills" else "Facturas"
        ExpenseType. SUPPLIES -> if (isEnglish) "Supplies" else "Suministros"
        ExpenseType.OTHER -> if (isEnglish) "Other" else "Otro"
    }
}

fun getSaleTypeName(type:  SaleType, isEnglish: Boolean): String {
    return when (type) {
        SaleType.WATER_BY_BOTTLE -> if (isEnglish) "Water by Bottle" else "Agua por Botella"
        SaleType. WATER_BY_GALLON -> if (isEnglish) "Water by Gallon" else "Agua por Galón"
        SaleType.OTHER -> if (isEnglish) "Other" else "Otro"
    }
}

// ==================== CATEGORIES SCREEN (Saved Reports) ====================

@Composable
fun CategoriesScreen(language: Language) {
    val isEnglish = language == Language. ENGLISH

    val savedReports = remember {
        listOf(
            SavedReport(
                name = "Category 1",
                dateCreated = Date(),
                status = ReportStatus.COMPLETED,
                owner = "John Doe"
            ),
            SavedReport(
                name = if (isEnglish) "Monthly Summary" else "Resumen Mensual",
                dateCreated = Date(System.currentTimeMillis() - 86400000 * 7),
                status = ReportStatus.IN_PROGRESS,
                owner = "Jane Smith"
            ),
            SavedReport(
                name = if (isEnglish) "Quarterly Report" else "Informe Trimestral",
                dateCreated = Date(System.currentTimeMillis() - 86400000 * 30),
                status = ReportStatus. DRAFT,
                owner = "Mike Johnson"
            )
        )
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Saved Reports" else "Informes Guardados",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors. TextPrimary
                )
                Button(
                    onClick = { /* New report action */ },
                    colors = ButtonDefaults. buttonColors(containerColor = AppColors.Primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isEnglish) "New Report" else "Nuevo Informe")
                }
            }
        }

        items(savedReports) { report ->
            SavedReportCard(
                report = report,
                isEnglish = isEnglish,
                dateFormat = dateFormat
            )
        }
    }
}

@Composable
fun SavedReportCard(
    report: SavedReport,
    isEnglish: Boolean,
    dateFormat:  SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors. BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors. Surface)
    ) {
        Column(modifier = Modifier. padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors. TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "${if (isEnglish) "Created" else "Creado"}:  ${dateFormat.format(report.dateCreated)}",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${if (isEnglish) "Owner" else "Propietario"}: ${report.owner}",
                            color = AppColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(status = report.status, isEnglish = isEnglish)
                    Spacer(Modifier. width(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { /* View */ }) {
                            Icon(Icons.Default. Visibility, contentDescription = "View", tint = AppColors.TextSecondary)
                        }
                        IconButton(onClick = { /* Edit */ }) {
                            Icon(Icons.Default. Edit, contentDescription = "Edit", tint = AppColors.TextSecondary)
                        }
                        IconButton(onClick = { /* Delete */ }) {
                            Icon(Icons.Default. Delete, contentDescription = "Delete", tint = AppColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: ReportStatus, isEnglish:  Boolean) {
    val (backgroundColor, textColor, text) = when (status) {
        ReportStatus.COMPLETED -> Triple(
            Color(0xFFDCFCE7),
            Color(0xFF15803D),
            if (isEnglish) "Completed" else "Completado"
        )
        ReportStatus. IN_PROGRESS -> Triple(
            Color(0xFFFEF9C3),
            Color(0xFFA16207),
            if (isEnglish) "In Progress" else "En Progreso"
        )
        ReportStatus.DRAFT -> Triple(
            AppColors.CardBackground,
            AppColors.TextSecondary,
            if (isEnglish) "Draft" else "Borrador"
        )
    }

    Box(
        modifier = Modifier
            . background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// ==================== PROJECT MANAGEMENT SCREEN (Monthly Sales Revenue) ====================

@Composable
fun ProjectManagementScreen(language: Language) {
    val isEnglish = language == Language.ENGLISH

    var monthlyRevenueData by remember {
        mutableStateOf(
            listOf(
                MonthlyRevenue("January", 2024, 12500.00, 8200.00),
                MonthlyRevenue("February", 2024, 14200.00, 9100.00),
                MonthlyRevenue("March", 2024, 11800.00, 7500.00),
                MonthlyRevenue("April", 2024, 15600.00, 10200.00),
                MonthlyRevenue("May", 2024, 13400.00, 8800.00),
                MonthlyRevenue("June", 2024, 16800.00, 11500.00)
            )
        )
    }

    val totalRevenue = monthlyRevenueData. sumOf { it.revenue }
    val totalExpenses = monthlyRevenueData. sumOf { it. expenses }
    val totalProfit = totalRevenue - totalExpenses
    val avgMonthlyRevenue = totalRevenue / monthlyRevenueData.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isEnglish) "Monthly Sales Revenue Tracking" else "Seguimiento de Ingresos de Ventas Mensuales",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
        }

        // Summary Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProjectSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "Total Revenue" else "Ingresos Totales",
                    value = "$${String.format("%,.2f", totalRevenue)}",
                    icon = Icons.Default.TrendingUp,
                    iconColor = AppColors. SuccessGreen
                )
                ProjectSummaryCard(
                    modifier = Modifier. weight(1f),
                    title = if (isEnglish) "Total Profit" else "Ganancia Total",
                    value = "$${String. format("%,.2f", totalProfit)}",
                    icon = Icons.Default. AttachMoney,
                    iconColor = AppColors.Primary
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProjectSummaryCard(
                    modifier = Modifier. weight(1f),
                    title = if (isEnglish) "Total Expenses" else "Gastos Totales",
                    value = "$${String.format("%,.2f", totalExpenses)}",
                    icon = Icons.Default.TrendingDown,
                    iconColor = AppColors. ErrorRed
                )
                ProjectSummaryCard(
                    modifier = Modifier.weight(1f),
                    title = if (isEnglish) "Avg Monthly" else "Promedio Mensual",
                    value = "$${String.format("%,.2f", avgMonthlyRevenue)}",
                    icon = Icons.Default. BarChart,
                    iconColor = AppColors. WarningYellow
                )
            }
        }

        // Monthly Breakdown Header
        item {
            Text(
                text = if (isEnglish) "Monthly Breakdown" else "Desglose Mensual",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.TextPrimary
            )
        }

        // Monthly Revenue Cards
        items(monthlyRevenueData) { monthData ->
            MonthlyRevenueCard(
                monthData = monthData,
                isEnglish = isEnglish
            )
        }

        // Add New Month Entry
        item {
            AddMonthCard(
                isEnglish = isEnglish,
                onAddMonth = { month, revenue, expenses ->
                    monthlyRevenueData = monthlyRevenueData + MonthlyRevenue(
                        month = month,
                        year = 2024,
                        revenue = revenue,
                        expenses = expenses
                    )
                }
            )
        }
    }
}

@Composable
fun ProjectSummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconColor. copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }
            Spacer(Modifier. height(12.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }
    }
}

@Composable
fun MonthlyRevenueCard(
    monthData: MonthlyRevenue,
    isEnglish: Boolean
) {
    val profit = monthData.revenue - monthData.expenses
    val profitMargin = (profit / monthData.revenue) * 100

    val monthNameLocalized = if (isEnglish) {
        monthData.month
    } else {
        when (monthData.month) {
            "January" -> "Enero"
            "February" -> "Febrero"
            "March" -> "Marzo"
            "April" -> "Abril"
            "May" -> "Mayo"
            "June" -> "Junio"
            "July" -> "Julio"
            "August" -> "Agosto"
            "September" -> "Septiembre"
            "October" -> "Octubre"
            "November" -> "Noviembre"
            "December" -> "Diciembre"
            else -> monthData. month
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.BorderColor),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$monthNameLocalized ${monthData.year}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight. SemiBold,
                    color = AppColors.TextPrimary
                )
                Box(
                    modifier = Modifier
                        .background(
                            if (profit >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${String.format("%.1f", profitMargin)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight. Medium,
                        color = if (profit >= 0) Color(0xFF15803D) else Color(0xFF991B1B)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Revenue" else "Ingresos",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "$${String.format("%,.2f", monthData.revenue)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors. SuccessGreen
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEnglish) "Expenses" else "Gastos",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "$${String. format("%,.2f", monthData.expenses)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.ErrorRed
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isEnglish) "Profit" else "Ganancia",
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "$${String.format("%,.2f", profit)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors. Primary
                    )
                }
            }

            // Progress bar for expense ratio
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (isEnglish) "Expense Ratio" else "Ratio de Gastos",
                fontSize = 12.sp,
                color = AppColors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (monthData.expenses / monthData.revenue).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    monthData.expenses / monthData.revenue < 0.6 -> AppColors. SuccessGreen
                    monthData.expenses / monthData.revenue < 0.8 -> AppColors. WarningYellow
                    else -> AppColors.ErrorRed
                },
                trackColor = AppColors.BorderColor
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMonthCard(
    isEnglish: Boolean,
    onAddMonth:  (String, Double, Double) -> Unit
) {
    var selectedMonth by remember { mutableStateOf("") }
    var revenueInput by remember { mutableStateOf("") }
    var expensesInput by remember { mutableStateOf("") }
    var monthExpanded by remember { mutableStateOf(false) }

    val months = if (isEnglish) {
        listOf("January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
    } else {
        listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")
    }

    Card(
        modifier = Modifier. fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppColors.Primary),
        colors = CardDefaults.cardColors(containerColor = AppColors.Primary. copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isEnglish) "Add New Month Entry" else "Agregar Nuevo Mes",
                fontSize = 18.sp,
                fontWeight = FontWeight. SemiBold,
                color = AppColors.Primary
            )

            Spacer(Modifier.height(16.dp))

            // Month Dropdown
            ExposedDropdownMenuBox(
                expanded = monthExpanded,
                onExpandedChange = { monthExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isEnglish) "Select Month" else "Seleccionar Mes") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = monthExpanded,
                    onDismissRequest = { monthExpanded = false }
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                selectedMonth = month
                                monthExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = revenueInput,
                    onValueChange = { revenueInput = it },
                    label = { Text(if (isEnglish) "Revenue" else "Ingresos") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType. Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = expensesInput,
                    onValueChange = { expensesInput = it },
                    label = { Text(if (isEnglish) "Expenses" else "Gastos") },
                    leadingIcon = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier. height(16.dp))

            Button(
                onClick = {
                    val revenue = revenueInput.toDoubleOrNull()
                    val expenses = expensesInput.toDoubleOrNull()
                    if (selectedMonth. isNotBlank() && revenue != null && expenses != null) {
                        onAddMonth(selectedMonth, revenue, expenses)
                        selectedMonth = ""
                        revenueInput = ""
                        expensesInput = ""
                    }
                },
                modifier = Modifier. fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors. Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default. Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isEnglish) "Add Month" else "Agregar Mes")
            }
        }
    }
}
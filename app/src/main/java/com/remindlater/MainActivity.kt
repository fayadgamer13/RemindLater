package com.remindlater

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.remindlater.ui.theme.RemindLaterTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var settingsManager: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        
        applyLocale(settingsManager.language)
        
        enableEdgeToEdge()
        setContent {
            val themeMode = remember { mutableStateOf(settingsManager.themeMode) }
            val useDynamicColor = remember { mutableStateOf(settingsManager.useDynamicColor) }
            
            val darkTheme = when (themeMode.value) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            RemindLaterTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor.value) {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        ReminderApp(navController = navController)
                    }
                    composable("settings") {
                        SettingsScreen(
                            navController = navController,
                            settingsManager = settingsManager,
                            onThemeChange = { themeMode.value = it },
                            onDynamicColorChange = { useDynamicColor.value = it },
                            onLanguageChange = { 
                                applyLocale(it)
                                recreate()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun applyLocale(languageCode: String) {
        val locale = if (languageCode == "auto") {
            Locale.getDefault()
        } else {
            Locale(languageCode)
        }
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderApp(
    viewModel: ReminderViewModel = viewModel(),
    navController: NavHostController
) {
    val reminders by viewModel.allReminders.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(id = R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.new_reminder))
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.no_reminders))
                }
            } else {
                LazyColumn {
                    items(reminders) { reminder ->
                        ReminderItem(
                            reminder = reminder,
                            onDelete = { viewModel.delete(reminder) },
                            onToggleComplete = { viewModel.update(reminder.copy(isCompleted = !reminder.isCompleted)) }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddReminderDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, desc, time, isVoice ->
                    viewModel.insert(Reminder(title = title, description = desc, dateTime = time, isVoiceReminder = isVoice))
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ReminderItem(reminder: Reminder, onDelete: () -> Unit, onToggleComplete: () -> Unit) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(reminder.dateTime))

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = reminder.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = reminder.title, style = MaterialTheme.typography.titleMedium)
                    if (reminder.isVoiceReminder) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voice", modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = reminder.description, style = MaterialTheme.typography.bodyMedium)
                Text(text = dateString, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(onDismiss: () -> Unit, onConfirm: (String, String, Long, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var isVoiceReminder by remember { mutableStateOf(false) }
    
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    var selectedDate by remember { mutableStateOf<Long?>(null) }
    var selectedTimeStr by remember { mutableStateOf("") }
    val defaultTimeStr = stringResource(id = R.string.select_time)
    val effectiveTimeStr = if (selectedTimeStr.isEmpty()) defaultTimeStr else selectedTimeStr

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.new_reminder)) },
        text = {
            Column {
                TextField(value = title, onValueChange = { title = it }, label = { Text(stringResource(id = R.string.title)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                TextField(value = desc, onValueChange = { desc = it }, label = { Text(stringResource(id = R.string.description)) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.voice_reminder))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(checked = isVoiceReminder, onCheckedChange = { isVoiceReminder = it })
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedDate == null) stringResource(id = R.string.select_date) else SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate!!)))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(effectiveTimeStr)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && selectedDate != null,
                onClick = {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = selectedDate!!
                    calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    calendar.set(Calendar.MINUTE, timePickerState.minute)
                    calendar.set(Calendar.SECOND, 0)
                    onConfirm(title, desc, calendar.timeInMillis, isVoiceReminder)
                }
            ) { Text(stringResource(id = R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.cancel)) } }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text(stringResource(id = R.string.ok)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTimeStr = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(id = R.string.ok)) }
            },
            text = { 
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState) 
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    settingsManager: SettingsManager,
    onThemeChange: (String) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    val context = LocalContext.current
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val version = packageInfo.versionName

    var useDefaultAlarm by remember { mutableStateOf(settingsManager.useDefaultAlarm) }
    var customAlarmUri by remember { mutableStateOf(settingsManager.customAlarmUri) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                settingsManager.customAlarmUri = it.toString()
                customAlarmUri = it.toString()
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            Text(stringResource(id = R.string.theme))
            var themeExpanded by remember { mutableStateOf(false) }
            val themes = listOf(
                Pair("auto", stringResource(id = R.string.theme_auto)),
                Pair("light", stringResource(id = R.string.theme_light)),
                Pair("dark", stringResource(id = R.string.theme_dark))
            )
            
            Box {
                Button(onClick = { themeExpanded = true }) {
                    val currentThemeName = themes.find { it.first == settingsManager.themeMode }?.second ?: themes[0].second
                    Text(currentThemeName)
                }
                DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                    themes.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                settingsManager.themeMode = code
                                onThemeChange(code)
                                themeExpanded = false
                            }
                        )
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(id = R.string.dynamic_color))
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = settingsManager.useDynamicColor,
                        onCheckedChange = { 
                            settingsManager.useDynamicColor = it
                            onDynamicColorChange(it)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(stringResource(id = R.string.language))
            var langExpanded by remember { mutableStateOf(false) }
            val languages = listOf(
                Pair("auto", stringResource(id = R.string.auto_language)),
                Pair("en", "English"),
                Pair("es", "Español"),
                Pair("ar", "العربية"),
                Pair("fr", "Français"),
                Pair("zh", "中文"),
                Pair("ja", "日本語"),
                Pair("ko", "한국어"),
                Pair("hi", "हिन्दी"),
                Pair("bn", "বাংলা"),
                Pair("pt", "Português"),
                Pair("ru", "Русский"),
                Pair("ur", "اردو"),
                Pair("tr", "Türkçe"),
                Pair("bg", "Български"),
                Pair("hr", "Hrvatski"),
                Pair("nl", "Nederlands"),
                Pair("de", "Deutsch"),
                Pair("id", "Bahasa Indonesia"),
                Pair("ga", "Gaeilge"),
                Pair("lo", "ລາວ"),
                Pair("ms", "Bahasa Melayu"),
                Pair("mn", "Монгол"),
                Pair("fa", "فارسی"),
                Pair("sr", "Српски"),
                Pair("vi", "Tiếng Việt")
            )
            
            Box {
                Button(onClick = { langExpanded = true }) {
                    val currentLangName = languages.find { it.first == settingsManager.language }?.second ?: languages[0].second
                    Text(currentLangName)
                }
                DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    languages.forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                settingsManager.language = code
                                onLanguageChange(code)
                                langExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(id = R.string.use_default_alarm))
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = useDefaultAlarm,
                    onCheckedChange = { 
                        settingsManager.useDefaultAlarm = it
                        useDefaultAlarm = it
                    }
                )
            }

            if (!useDefaultAlarm) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(id = R.string.pick_audio))
                }
                if (customAlarmUri != null) {
                    Text(
                        text = "Selected: ${Uri.parse(customAlarmUri).lastPathSegment}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("${stringResource(id = R.string.app_version)}: $version")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fayadgamer13/RemindLater"))
                context.startActivity(intent)
            }) {
                Text(stringResource(id = R.string.github_project))
            }
        }
    }
}

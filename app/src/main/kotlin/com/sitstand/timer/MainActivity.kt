package com.sitstand.timer

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sitstand.timer.data.TimerRepository
import com.sitstand.timer.model.TimerMode
import com.sitstand.timer.model.formatDuration
import com.sitstand.timer.service.NotificationHelper
import com.sitstand.timer.service.TimerService
import com.sitstand.timer.ui.theme.BambooLight
import com.sitstand.timer.ui.theme.BambooTone
import com.sitstand.timer.ui.theme.SitStandTimerTheme
import com.sitstand.timer.ui.theme.SlateBlueDeep
import com.sitstand.timer.ui.theme.SlateBlueMatte
import com.sitstand.timer.ui.theme.WarmWhite
import com.sitstand.timer.widget.TimerWidgetProvider
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TimerRepository.init(this)
        NotificationHelper.ensureChannels(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            SitStandTimerTheme {
                SitStandScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitStandScreen() {
    val context = LocalContext.current
    val state by TimerRepository.state.collectAsStateWithLifecycle()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isRunning, state.endTimeEpochMs) {
        nowMs = System.currentTimeMillis()
        while (state.isRunning) {
            delay(500L)
            nowMs = System.currentTimeMillis()
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var needsExactAlarmPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager != null &&
                !alarmManager.canScheduleExactAlarms()
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
                needsExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    alarmManager != null &&
                    !alarmManager.canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isStand = state.mode == TimerMode.STAND
    val heroContainerColor = if (isStand) BambooTone else SlateBlueMatte
    val heroContentColor = if (isStand) SlateBlueDeep else WarmWhite
    val heroButtonContainerColor = if (isStand) SlateBlueMatte else BambooTone
    val heroButtonContentColor = if (isStand) WarmWhite else SlateBlueDeep
    val heroOutlineColor = if (isStand) SlateBlueMatte else BambooLight

    val statusText = when {
        state.isRunning && isStand -> "STANDING"
        state.isRunning && !isStand -> "SITTING"
        isStand -> "READY TO STAND"
        else -> "READY TO SIT"
    }

    val remainingMs = state.remainingMs(nowMs)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Sit/Stand Timer",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Allow notifications to see the lock screen timer and switch alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        TextButton(
                            onClick = {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        ) {
                            Text("Allow")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = heroContainerColor,
                    contentColor = heroContentColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = heroContentColor.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatDuration(remainingMs),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = heroContentColor
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!state.isRunning &&
                                    !hasNotificationPermission &&
                                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                ) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                TimerService.toggle(context)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = heroButtonContainerColor,
                                contentColor = heroButtonContentColor
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (state.isRunning) "Stop" else "Start",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { TimerService.switchMode(context) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = heroOutlineColor
                            ),
                            border = BorderStroke(1.dp, heroOutlineColor.copy(alpha = 0.7f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Switch Mode")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DurationStepper(
                        title = "Sit Duration",
                        minutes = state.sitDurationMinutes,
                        presets = listOf(30, 45, 60),
                        onChange = { newMinutes ->
                            TimerRepository.updateSitDuration(context, newMinutes)
                            TimerWidgetProvider.updateAllWidgets(context)
                        }
                    )

                    HorizontalDivider()

                    DurationStepper(
                        title = "Stand Duration",
                        minutes = state.standDurationMinutes,
                        presets = listOf(15, 30, 45),
                        onChange = { newMinutes ->
                            TimerRepository.updateStandDuration(context, newMinutes)
                            TimerWidgetProvider.updateAllWidgets(context)
                        }
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Automatically start next interval",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Switch between Sit and Stand without tapping Start",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.autoAdvance,
                            onCheckedChange = { enabled ->
                                TimerRepository.setAutoAdvance(context, enabled)
                            }
                        )
                    }
                }
            }

            if (needsExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Grant exact alarm access so switch alerts fire on time during Doze.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Settings")
                        }
                    }
                }
            }

            Text(
                text = "Tip: Add the Sit/Stand Timer widget to your home screen for one-tap start/stop.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DurationStepper(
    title: String,
    minutes: Int,
    presets: List<Int>,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "$minutes min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onChange(minutes - 5) },
                modifier = Modifier.weight(1f)
            ) {
                Text("-5")
            }
            OutlinedButton(
                onClick = { onChange(minutes - 1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("-1")
            }
            OutlinedButton(
                onClick = { onChange(minutes + 1) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+1")
            }
            OutlinedButton(
                onClick = { onChange(minutes + 5) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+5")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            presets.forEach { preset ->
                FilterChip(
                    selected = minutes == preset,
                    onClick = { onChange(preset) },
                    label = { Text("${preset}m") }
                )
            }
        }
    }
}

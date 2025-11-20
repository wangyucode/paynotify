package cn.wycode.pay_notify

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import cn.wycode.pay_notify.ui.theme.PayNotifyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayNotifyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NotificationLogScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

    }
}

@Composable
fun NotificationLogScreen(modifier: Modifier = Modifier) {
    val notifications = remember { mutableStateListOf<String>() }
    var isServiceEnabled by remember { mutableStateOf(false) }
    var isProduction by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    // 获取当前上下文
    val context = LocalContext.current

    // 请求通知权限的Launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    // 检查通知权限
    fun checkNotificationPermission() {
        hasNotificationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // 请求通知权限
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 检查当前环境设置
    fun checkCurrentEnvironment() {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        isProduction = sharedPreferences.getBoolean("isProduction", false)
    }

    // 检查通知监听服务是否已启用
    fun checkNotificationListenerPermission() {
        val packageName = "cn.wycode.pay_notify"
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        isServiceEnabled = flat?.contains(packageName) == true
    }

    // 请求用户授权通知监听
    fun requestNotificationListenerPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        context.startActivity(intent)
    }

    // 切换环境
    fun toggleEnvironment(isProduction: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("isProduction", isProduction)
            .apply()
    }

    // 初始化通知监听回调
    LaunchedEffect(Unit) {
        // 同步现有通知
        notifications.addAll(NotificationListener.notifications)

        // 设置通知接收回调
        NotificationListener.onNotificationReceived = { notificationInfo ->
            notifications.add(0, notificationInfo)
        }
//        checkNotificationPermission()
        checkNotificationListenerPermission()
        checkCurrentEnvironment()

        // 如果没有通知权限，请求权限
//        if (!hasNotificationPermission) {
//            requestNotificationPermission()
//        }

        if (!isServiceEnabled) {
            // 如果服务未启用，尝试请求权限
            requestNotificationListenerPermission()
        }
    }

    LifecycleResumeEffect(Unit) {
//        checkNotificationPermission()
        checkNotificationListenerPermission()
        onPauseOrDispose {}
    }

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        ) {
            items(notifications) { notification ->
                Column {
                    Text(
                        text = notification,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(thickness = Dp.Hairline)
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isServiceEnabled) {
                Button(onClick = { requestNotificationListenerPermission() }) {
                    Text(text = "开启监听🔔")
                }
            } else {
                Text(text = "enabled✅")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = if (isProduction) "prod" else "dev")
                Switch(
                    checked = isProduction,
                    onCheckedChange = {
                        isProduction = it
                        toggleEnvironment(it)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationLogScreenPreview() {
    PayNotifyTheme {
        NotificationLogScreen()
    }
}
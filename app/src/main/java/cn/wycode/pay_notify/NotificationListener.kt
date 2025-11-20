package cn.wycode.pay_notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class NotificationListener : NotificationListenerService() {

    companion object {
        val notifications = mutableListOf<String>()
        var onNotificationReceived: ((String) -> Unit)? = null
        private const val CHANNEL_ID = "notification_listener_channel"
        private const val NOTIFICATION_ID = 1001

        private const val TAG = "NotificationListener"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        // 创建通知渠道（Android 8.0+）
//        createNotificationChannel()
        // 启动前台服务
//        startForegroundService()
    }

    private fun createNotificationChannel() {
        val name = "通知监听服务"
        val descriptionText = "用于持续监听系统通知"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // 注册通知渠道
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundService() {
        val notificationBuilder = Notification.Builder(this, CHANNEL_ID)


        val notification = notificationBuilder
            .setContentTitle("青衿AI支付助手")
            .setContentText("正在监听...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(TAG, "收到通知: $sbn")
        val packageName = sbn.packageName
        if (!packageName.equals("com.tencent.mm")) return

        val notification = sbn.notification
        val postTime = sbn.postTime
        val title = notification.extras.getString("android.title", "")
//        if ("微信收款助手" != title) return

        val text = notification.extras.getString("android.text", "")
        val currentTime =
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(postTime))
        val notificationInfo = "$currentTime: $packageName\nTitle: $title\nText: $text"
        // 从"微信支付收款0.01元"中提取金额
//        val amountRegex = "收款(\\d+\\.\\d+)元".toRegex()
//        val matchResult = amountRegex.find(text) ?: return

//        val amount = matchResult.groupValues[1]
        val amount = "11.11" // 测试固定金额
        Log.d(TAG, notificationInfo)
        Toast.makeText(this, "收到通知:\n$notificationInfo", Toast.LENGTH_LONG).show()
        // 添加到通知列表
        notifications.add(0, notificationInfo) // 添加到开头，最新的在前面
        // 添加到界面
        onNotificationReceived?.invoke(notificationInfo)
        // 发送到服务器
        sendNotificationToServer(amount, postTime)
    }

    private fun sendNotificationToServer(amount: String, postTime: Long) {
        val serverHost = getServerHost()
        val jsonMediaType = "application/json; charset=utf-8".toMediaType()

        // 创建签名
        val secretKey = BuildConfig.SECRET_KEY // 从BuildConfig读取密钥，避免硬编码
        val message = "$amount$postTime" // 组合参数作为签名原串
        val signature = generateHmacSha256Signature(message, secretKey)

        val jsonBody =
            "{\"amount\": \"$amount\", \"time\": $postTime, \"signature\": \"$signature\"}"
        val requestBody = jsonBody.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$serverHost/payment")
            .post(requestBody)
            .build()

        Log.d(TAG, "sendNotificationToServer: ${request.url} , body: $jsonBody")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Log.d(TAG, "通知发送到服务器成功")
                } else {
                    Log.e(TAG, "通知发送到服务器失败: ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "发送通知到服务器时出错: ${e.message}")
            }
        }
    }

    /**
     * 生成HMAC-SHA256签名
     * @param message 要签名的消息
     * @param secretKey 密钥
     * @return 签名后的十六进制字符串
     */
    private fun generateHmacSha256Signature(message: String, secretKey: String): String {
        try {
            val algorithm = "HmacSHA256"
            val mac = javax.crypto.Mac.getInstance(algorithm)
            val secretKeySpec = javax.crypto.spec.SecretKeySpec(secretKey.toByteArray(), algorithm)
            mac.init(secretKeySpec)
            val hash = mac.doFinal(message.toByteArray())

            // 将字节数组转换为十六进制字符串
            return hash.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            Log.e(TAG, "生成签名时出错: ${e.message}")
            return ""
        }
    }

    private fun getServerHost(): String {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("app_settings", MODE_PRIVATE)
        val isProduction = sharedPreferences.getBoolean("isProduction", false)
        val host = if (isProduction) BuildConfig.HOST_PROD else BuildConfig.HOST_DEV
        return host
    }
}
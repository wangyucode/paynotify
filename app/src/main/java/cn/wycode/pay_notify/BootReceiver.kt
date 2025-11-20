package cn.wycode.pay_notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("BootReceiver", "设备开机完成，尝试启动通知监听服务")
            // 启动通知监听服务
            val serviceIntent = Intent(context, NotificationListener::class.java)
            // 对于NotificationListenerService，应该使用startService而不是startForegroundService
            // 因为它有自己的绑定机制
            context.startService(serviceIntent)
        }
    }
}
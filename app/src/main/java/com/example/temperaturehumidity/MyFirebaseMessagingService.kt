package com.example.temperaturehumidity

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Token mới: $token")
        // Gửi token đến server nếu cần
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Kiểm tra nếu có dữ liệu trong thông báo
        remoteMessage.data.isNotEmpty().let {
            Log.d("FCM", "Dữ liệu nhận được: ${remoteMessage.data}")
        }

        // Kiểm tra nếu có thông báo hiển thị
        remoteMessage.notification?.let {
            Log.d("FCM", "Thông báo nhận được: ${it.body}")
        }
    }
}
package com.example.temperaturehumidity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import com.example.temperaturehumidity.ui.theme.TemperaturehumidityTheme
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Lắng nghe dữ liệu từ Firebase cho cảnh báo mưa
        database = FirebaseDatabase.getInstance().reference.child("RainSensor").child("isRaining")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isRaining = snapshot.getValue(Boolean::class.java) ?: false
                Log.d("Firebase", "Trạng thái mưa: $isRaining")

                if (isRaining) {
                    sendNotification("Warning!", "It's raining, check now!")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Lỗi khi đọc dữ liệu: ${error.message}")
            }
        })

        setContent {
            TemperaturehumidityTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TemperatureHumidityScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "rain_alert_channel"

        // Đường dẫn file âm thanh tùy chỉnh
        val soundUri = Uri.parse("android.resource://${packageName}/raw/rain_alert")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(
                channelId,
                "Cảnh báo thời tiết",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(soundUri, audioAttributes)
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.deleteNotificationChannel(channelId)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}

@Composable
fun TemperatureHumidityScreen(modifier: Modifier = Modifier) {
    var temperature by remember { mutableStateOf("--") }
    var humidity by remember { mutableStateOf("--") }
    var isLightOn by remember { mutableStateOf(false) }
    val context = LocalContext.current // Lấy context từ Composable

    val database = FirebaseDatabase.getInstance()
    val sensorRef = database.getReference("sensor")
    val lightRef = database.getReference("light")

    // Lắng nghe nhiệt độ và độ ẩm
    LaunchedEffect(Unit) {
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.child("temperature").getValue(Double::class.java)
                val hum = snapshot.child("humidity").getValue(Int::class.java)
                if (temp != null && hum != null) {
                    temperature = "$temp"
                    humidity = "$hum"
                    // Kiểm tra nếu nhiệt độ > 30 độ thì gửi thông báo
                    if (temp > 30) {
                        (context as? MainActivity)?.sendNotification(
                            "Warning!",
                            "Temperatures exceed 30°C, currently $temp°C!"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                temperature = "Error"
                humidity = "Error"
            }
        })

        // Lắng nghe trạng thái đèn
        lightRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(Boolean::class.java)
                isLightOn = state == true
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Lỗi khi đọc dữ liệu: ${error.message}")
            }
        })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0288D1), Color(0xFF4FC3F7))
                )
            )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            TemperatureDisplay(temperature)
            Spacer(modifier = Modifier.height(24.dp))
            HumidityDisplay(humidity)
            Spacer(modifier = Modifier.height(24.dp))
            LightControl(isLightOn) { newState ->
                lightRef.setValue(newState)
                isLightOn = newState
            }
        }
    }
}

@Composable
fun TemperatureDisplay(temperature: String) {
    val temperatureValue = temperature.toFloatOrNull() ?: 0f
    val startColor = Color.Green
    val midColor = Color.Yellow
    val endColor = Color.Red
    val N = 30f
    val backgroundColor = if (temperatureValue <= N) {
        lerpColor(startColor, midColor, endColor, temperatureValue / N)
    } else {
        endColor
    }

    Box(
        modifier = Modifier
            .size(150.dp) // Hình vuông
            .background(
                brush = Brush.linearGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.8f))),
                shape = RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$temperature°C",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Temperature",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

@Composable
fun HumidityDisplay(humidity: String) {
    val humidityValue = humidity.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val startColor = Color(0xFFE0F7FA)
    val endColor = Color(0xFF01579B)
    val backgroundColor = lerp(startColor, endColor, humidityValue / 100f)

    Box(
        modifier = Modifier
            .size(150.dp) // Hình vuông
            .background(
                brush = Brush.linearGradient(listOf(backgroundColor, backgroundColor.copy(alpha = 0.8f))),
                shape = RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$humidity%",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "Humidity",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
        )
    }
}

@Composable
fun LightControl(isLightOn: Boolean, onStateChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .size(150.dp) // Hình vuông
            .background(
                brush = Brush.linearGradient(
                    colors = if (isLightOn)
                        listOf(Color(0xFFFFC107), Color(0xFFFFE082))
                    else
                        listOf(Color(0xFF757575), Color(0xFFB0BEC5))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = { onStateChange(!isLightOn) },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isLightOn) "ON" else "OFF",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Light",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Hàm lerpColor (giữ nguyên từ code cũ)
fun lerpColor(startColor: Color, midColor: Color, endColor: Color, fraction: Float): Color {
    val color1 = if (fraction < 0.5) {
        val normalizedFraction = fraction * 2
        lerp(startColor, midColor, normalizedFraction)
    } else {
        val normalizedFraction = (fraction - 0.5f) * 2
        lerp(midColor, endColor, normalizedFraction)
    }
    return color1
}

fun lerp(startColor: Color, endColor: Color, fraction: Float): Color {
    val red = (startColor.red + fraction * (endColor.red - startColor.red)).coerceIn(0f, 1f)
    val green = (startColor.green + fraction * (endColor.green - startColor.green)).coerceIn(0f, 1f)
    val blue = (startColor.blue + fraction * (endColor.blue - startColor.blue)).coerceIn(0f, 1f)
    return Color(red, green, blue)
}

@Preview(showBackground = true)
@Composable
fun PreviewTemperatureHumidityScreen() {
    TemperaturehumidityTheme {
        TemperatureHumidityScreen()
    }
}
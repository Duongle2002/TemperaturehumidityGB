package com.example.temperaturehumidity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.temperaturehumidity.ui.theme.TemperaturehumidityTheme
import com.google.firebase.database.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TemperaturehumidityTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TemperatureHumidityScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TemperatureHumidityScreen(modifier: Modifier = Modifier) {
    var temperature by remember { mutableStateOf("--") }
    var humidity by remember { mutableStateOf("--") }

    // Kết nối Firebase
    val database = FirebaseDatabase.getInstance()
    val sensorRef = database.getReference("sensor")

    // Lắng nghe dữ liệu thay đổi từ Firebase
    LaunchedEffect(Unit) {
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.child("temperature").getValue(Double::class.java)
                val hum = snapshot.child("humidity").getValue(Int::class.java)

                if (temp != null && hum != null) {
                    temperature = "$temp °C"
                    humidity = "$hum %"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                temperature = "Error"
                humidity = "Error"
            }
        })
    }

    val image = painterResource(id = R.drawable.r) // Đặt hình nền

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.7f,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            TemperatureDisplay(temperature)
            Spacer(modifier = Modifier.height(16.dp))
            HumidityDisplay(humidity)
        }
    }
}

@Composable
fun TemperatureDisplay(temperature: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(80.dp)
            .background(Color.Red, RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nhiệt độ: $temperature",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
fun HumidityDisplay(humidity: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(80.dp)
            .background(Color.Blue, RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Độ ẩm: $humidity",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTemperatureHumidityScreen() {
    TemperaturehumidityTheme {
        TemperatureHumidityScreen()
    }
}

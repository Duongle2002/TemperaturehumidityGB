package com.example.temperaturehumidity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun TemperatureHumidityScreen(modifier: Modifier = Modifier) {
    var temperature by remember { mutableStateOf("--") }
    var humidity by remember { mutableStateOf("--") }

    val database = FirebaseDatabase.getInstance()
    val sensorRef = database.getReference("sensor")

    LaunchedEffect(Unit) {
        sensorRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.child("temperature").getValue(Double::class.java)
                val hum = snapshot.child("humidity").getValue(Int::class.java)
                if (temp != null && hum != null) {
                    temperature = "$temp"
                    humidity = "$hum"
                }
            }
            override fun onCancelled(error: DatabaseError) {
                temperature = "Error"
                humidity = "Error"
            }
        })
    }

    val image = painterResource(id = R.drawable.background)

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
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
            .height(80.dp)
            .background(backgroundColor, RectangleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nhiệt độ: $temperature°C",
            color = Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
fun HumidityDisplay(humidity: String, modifier: Modifier = Modifier) {
    val humidityValue = humidity.toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
    val startColor = Color(0xFFE0F7FA) // Màu nhạt nhất
    val endColor = Color(0xFF01579B) // Màu đậm nhất

    val backgroundColor = lerp(startColor, endColor, humidityValue / 100f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(80.dp)
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(2.dp, Color(0x00E0F7FA), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Độ ẩm: $humidity%",
            color = Color.Black,
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

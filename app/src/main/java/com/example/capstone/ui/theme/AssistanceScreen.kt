package com.example.capstone.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AssistanceScreen(
    onNavigate: (String) -> Unit
) {
    var step by remember { mutableStateOf("input") }
    var formData by remember {
        mutableStateOf(
            AssistantForm(
                event = "",
                theme = "",
                weather = "",
                temperature = ""
            )
        )
    }

    val scope = rememberCoroutineScope()

    val eventTypes = listOf(
        "Casual outing", "Work meeting", "Date night", "Party", "Wedding",
        "Gym/Sports", "Beach/Pool", "Shopping", "Concert", "Dinner"
    )
    val themes = listOf(
        "Casual", "Professional", "Formal", "Chic", "Sporty",
        "Bohemian", "Minimalist", "Trendy", "Classic", "Edgy"
    )
    val weatherConditions = listOf(
        "Sunny", "Cloudy", "Rainy", "Windy", "Humid", "Hot", "Cool", "Stormy"
    )

    fun handleGenerateRecommendation() {
        if (formData.event.isBlank() || formData.theme.isBlank() || formData.weather.isBlank()) {
            println("Please fill in all fields")
            return
        }
        step = "generating"
        scope.launch {
            delay(3000)
            step = "results"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
            .verticalScroll(rememberScrollState())
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onNavigate("home") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Style Assistant", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
                if (step == "input") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Let's create the perfect outfit for your occasion",
                        color = Color(0xFFE9D5FF)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            when (step) {
                "input" -> InputStep(
                    formData = formData,
                    onFormChange = { formData = it },
                    eventTypes = eventTypes,
                    themes = themes,
                    weatherConditions = weatherConditions,
                    onGenerate = { handleGenerateRecommendation() }
                )
                "generating" -> GeneratingStep()
                "results" -> ResultsStep(
                    formData = formData,
                    onNavigate = onNavigate,
                    onTryAgain = { step = "input" }
                )
            }
        }
    }
}

data class AssistantForm(
    val event: String,
    val theme: String,
    val weather: String,
    val temperature: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputStep(
    formData: AssistantForm,
    onFormChange: (AssistantForm) -> Unit,
    eventTypes: List<String>,
    themes: List<String>,
    weatherConditions: List<String>,
    onGenerate: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Style Assistant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text("Tell me about your occasion and I'll create the perfect outfit")

        // 🎉 Event Type
        Text("Event Type", fontWeight = FontWeight.Medium)
        var selectedEvent by remember { mutableStateOf(formData.event) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (row in eventTypes.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { event ->
                        val isSelected = selectedEvent == event
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(50)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFF9CA3AF),
                                    RoundedCornerShape(50)
                                )
                                .padding(vertical = 10.dp)
                                .clickable {
                                    selectedEvent = event
                                    onFormChange(formData.copy(event = event))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = event,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // 💅 Preferred Style Theme
        Text("Preferred Style Theme", fontWeight = FontWeight.Medium)
        var selectedTheme by remember { mutableStateOf(formData.theme) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (row in themes.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(50)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFF9CA3AF),
                                    RoundedCornerShape(50)
                                )
                                .padding(vertical = 10.dp)
                                .clickable {
                                    selectedTheme = theme
                                    onFormChange(formData.copy(theme = theme))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // 🌤 Weather
        Text("Current Weather", fontWeight = FontWeight.Medium)
        var selectedWeather by remember { mutableStateOf(formData.weather) }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (row in weatherConditions.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { weather ->
                        val isSelected = selectedWeather == weather
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(50)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF8B5CF6) else Color(0xFF9CA3AF),
                                    RoundedCornerShape(50)
                                )
                                .padding(vertical = 10.dp)
                                .clickable {
                                    selectedWeather = weather
                                    onFormChange(formData.copy(weather = weather))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = weather,
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // 🌡 Temperature
        ScrollableTemperaturePicker(
            value = formData.temperature,
            onValueChange = { onFormChange(formData.copy(temperature = it)) }
        )

        // 🧍‍♂️ Body Data Applied Section
        BodyDataAppliedBox(
            mapOf(
                "Shoulder Width" to "45 cm",
                "Hip Width" to "42 cm",
                "Torso Length" to "58 cm",
                "Leg Length" to "90 cm",
                "Arm Length" to "60 cm",
                "Shoulder-to-Hip Ratio" to "1.07",
                "Torso-to-Leg Ratio" to "0.64",
                "Arm-to-Leg Ratio" to "0.67",
                "Shoulder-to-Height Ratio" to "0.25",
                "Skin Color" to "Medium Tan"
            )
        )

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Text("Get My Outfit", color = Color.White)
        }
    }
}

@Composable
fun BodyDataAppliedBox(data: Map<String, String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text("Body Data Applied", fontWeight = FontWeight.SemiBold, color = Color(0xFF111827))
        Spacer(Modifier.height(8.dp))
        data.forEach { (label, value) ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = Color(0xFF4B5563))
                Text(value, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "These details are applied to personalize your recommendations.",
            fontSize = MaterialTheme.typography.bodySmall.fontSize,
            color = Color.Gray
        )
    }
}

@Composable
fun ScrollableTemperaturePicker(
    value: String,
    onValueChange: (String) -> Unit,
    minTemp: Int = 10,
    maxTemp: Int = 45
) {
    var temperature by remember { mutableStateOf(value.toIntOrNull() ?: 25) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Temperature (°C)", fontWeight = FontWeight.Medium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .background(Color.White, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.Thermostat, contentDescription = null, tint = Color.Gray)
                Spacer(Modifier.width(12.dp))
                Text("$temperature°C", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = {
                        if (temperature < maxTemp) {
                            temperature++
                            onValueChange(temperature.toString())
                        }
                    }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = Color.Gray)
                    }
                    IconButton(onClick = {
                        if (temperature > minTemp) {
                            temperature--
                            onValueChange(temperature.toString())
                        }
                    }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratingStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Color(0xFF8B5CF6))
        Text("Creating Your Perfect Look", style = MaterialTheme.typography.titleLarge)
        Text("Analyzing your style preferences and wardrobe...")
    }
}

@Composable
fun ResultsStep(
    formData: AssistantForm,
    onNavigate: (String) -> Unit,
    onTryAgain: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your Perfect Outfit", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Based on your ${formData.event} in ${formData.weather.lowercase()} weather")

        Box(
            modifier = Modifier
                .background(Color(0xFFD1FAE5), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("From Your Wardrobe", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                Spacer(Modifier.height(8.dp))
                Text("👔 Blue Blazer")
                Text("👖 Dark Jeans")
                Text("👞 Brown Loafers")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onNavigate("tryon") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Try On Virtual Mannequin")
                }
            }
        }

        Box(
            modifier = Modifier
                .background(Color(0xFFDBEAFE), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Alternative Options", fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                Spacer(Modifier.height(8.dp))
                Text("👕 White Cotton Shirt - $45 (Zara)")
                Text("🧥 Casual Cardigan - $65 (H&M)")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onTryAgain, modifier = Modifier.weight(1f)) {
                Text("Try Again")
            }
            Button(onClick = { onNavigate("history") }, modifier = Modifier.weight(1f)) {
                Text("Save to History")
            }
        }
    }
}

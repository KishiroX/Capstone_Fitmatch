package com.example.capstone.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Thermostat
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
        "Sunny", "Cloudy", "Rainy", "Snowy", "Windy", "Hot", "Cold", "Humid"
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
        // Header
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

        // Card container
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

// --- Data ---
data class AssistantForm(
    val event: String,
    val theme: String,
    val weather: String,
    val temperature: String
)

// --- Steps ---
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

        // Event
        OutlinedTextField(
            value = formData.event,
            onValueChange = { onFormChange(formData.copy(event = it)) },
            label = { Text("What's the occasion?") },
            placeholder = { Text("Select event type") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        // Theme
        OutlinedTextField(
            value = formData.theme,
            onValueChange = { onFormChange(formData.copy(theme = it)) },
            label = { Text("Preferred style theme?") },
            placeholder = { Text("Select style theme") },
            leadingIcon = { Icon(Icons.Default.Style, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        // Weather
        OutlinedTextField(
            value = formData.weather,
            onValueChange = { onFormChange(formData.copy(weather = it)) },
            label = { Text("Current weather?") },
            placeholder = { Text("Select weather condition") },
            leadingIcon = { Icon(Icons.Default.Cloud, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )

        // Temperature
        OutlinedTextField(
            value = formData.temperature,
            onValueChange = { onFormChange(formData.copy(temperature = it)) },
            label = { Text("Temperature (°C)") },
            placeholder = { Text("e.g. 22") },
            leadingIcon = { Icon(Icons.Default.Thermostat, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
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
fun GeneratingStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
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

        // Wardrobe suggestion
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
                Button(
                    onClick = { onNavigate("tryon") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try On Virtual Mannequin")
                }
            }
        }

        // Alternatives
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

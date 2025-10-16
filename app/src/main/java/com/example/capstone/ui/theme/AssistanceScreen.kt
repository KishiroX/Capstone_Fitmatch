package com.example.capstone.ui.theme

import android.content.Intent
import android.net.Uri
import android.util.Log
import coil.compose.AsyncImage
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.capstone.OutfitFetcher
import com.example.capstone.OnlineOutfit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ==================== Data Classes ====================
data class AssistantForm(
    val event: String,
    val theme: String,
    val weather: String,
    val temperature: String
)

// ==================== Input Step ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputStep(
    formData: AssistantForm,
    onFormChange: (AssistantForm) -> Unit,
    eventTypes: List<String>,
    themes: List<String>,
    weatherConditions: List<String>,
    onGenerate: () -> Unit,
    bodyAppliedData: Map<String, String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Style Assistant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text("Tell me about your occasion and I'll create the perfect outfit")

        // Event Type Selection
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

        // Theme Selection
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

        // Weather Selection
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

        // Temperature Picker
        ScrollableTemperaturePicker(
            value = formData.temperature,
            onValueChange = { onFormChange(formData.copy(temperature = it)) }
        )

        // Body Data
        BodyDataAppliedBox(bodyAppliedData)

        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
        ) {
            Text("Get My Outfit", color = Color.White)
        }
    }
}

// ==================== Assistance Screen ====================
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
                temperature = "25"
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

    val defaultBodyMap = mapOf(
        "Age" to "N/A",
        "Gender" to "N/A",
        "Weight" to "N/A",
        "Height" to "N/A",
        "Shoulder Width" to "N/A",
        "Hip Width" to "N/A",
        "Torso Length" to "N/A",
        "Leg Length" to "N/A",
        "Arm Length" to "N/A",
        "Shoulder-to-Hip Ratio" to "N/A",
        "Torso-to-Leg Ratio" to "N/A",
        "Arm-to-Leg Ratio" to "N/A",
        "Shoulder-to-Height Ratio" to "N/A",
        "Skin Color" to "N/A"
    )

    var bodyAppliedData by remember { mutableStateOf(defaultBodyMap) }
    var bodyLoading by remember { mutableStateOf(true) }

    var onlineRecommendations by remember { mutableStateOf(listOf<OnlineOutfit>()) }

    // ==================== Load Body Data ====================
    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            try {
                val userDoc = db.collection("users").document(uid).get().await()
                val bodyDoc = db.collection("users").document(uid)
                    .collection("bodyComposition").document("latest").get().await()

                val userMap = userDoc.data ?: emptyMap()
                val bodyMapRaw = bodyDoc.data ?: emptyMap()

                fun findInMap(map: Map<String, Any>, vararg candidates: String): String? {
                    for (cand in candidates) {
                        val exact = map.entries.firstOrNull { it.key.equals(cand, ignoreCase = true) }
                        if (exact != null) return exact.value.toString()
                        val normalizedCand = cand.replace("_", "").replace(" ", "").lowercase()
                        val fuzzy = map.entries.firstOrNull {
                            it.key.replace("_", "").replace(" ", "").lowercase() == normalizedCand
                        }
                        if (fuzzy != null) return fuzzy.value.toString()
                    }
                    return null
                }

                val finalMap = mutableMapOf<String, String>()
                finalMap["Age"] = findInMap(userMap, "age") ?: defaultBodyMap["Age"]!!
                finalMap["Gender"] = findInMap(userMap, "gender", "sex") ?: defaultBodyMap["Gender"]!!
                finalMap["Weight"] = findInMap(userMap, "weight") ?: defaultBodyMap["Weight"]!!
                finalMap["Height"] = findInMap(userMap, "height") ?: defaultBodyMap["Height"]!!
                finalMap["Shoulder Width"] = findInMap(bodyMapRaw, "Shoulder Width") ?: defaultBodyMap["Shoulder Width"]!!
                finalMap["Hip Width"] = findInMap(bodyMapRaw, "Hip Width") ?: defaultBodyMap["Hip Width"]!!
                finalMap["Torso Length"] = findInMap(bodyMapRaw, "Torso Length") ?: defaultBodyMap["Torso Length"]!!
                finalMap["Leg Length"] = findInMap(bodyMapRaw, "Leg Length") ?: defaultBodyMap["Leg Length"]!!
                finalMap["Arm Length"] = findInMap(bodyMapRaw, "Arm Length") ?: defaultBodyMap["Arm Length"]!!
                finalMap["Shoulder-to-Hip Ratio"] = findInMap(bodyMapRaw, "Shoulder-to-Hip Ratio") ?: defaultBodyMap["Shoulder-to-Hip Ratio"]!!
                finalMap["Torso-to-Leg Ratio"] = findInMap(bodyMapRaw, "Torso-to-Leg Ratio") ?: defaultBodyMap["Torso-to-Leg Ratio"]!!
                finalMap["Arm-to-Leg Ratio"] = findInMap(bodyMapRaw, "Arm-to-Leg Ratio") ?: defaultBodyMap["Arm-to-Leg Ratio"]!!
                finalMap["Shoulder-to-Height Ratio"] = findInMap(bodyMapRaw, "Shoulder-to-Height Ratio") ?: defaultBodyMap["Shoulder-to-Height Ratio"]!!
                finalMap["Skin Color"] = findInMap(bodyMapRaw, "Estimated Skin Color", "Skin Color") ?: defaultBodyMap["Skin Color"]!!

                bodyAppliedData = finalMap
            } catch (e: Exception) {
                bodyAppliedData = defaultBodyMap
            }
        } else {
            bodyAppliedData = defaultBodyMap
        }
        bodyLoading = false
    }

    // ==================== Recommendation Handler ====================
    fun handleGenerateRecommendation() {
        if (formData.event.isBlank() || formData.theme.isBlank() || formData.weather.isBlank()) return

        step = "generating"
        scope.launch {
            try {
                val outfits = OutfitFetcher.getOutfitRecommendations(
                    eventType = formData.event,
                    preferredStyle = formData.theme,
                    theme = formData.theme,
                    currentWeather = formData.weather,
                    temperature = formData.temperature.toDoubleOrNull() ?: 25.0,
                    bodyData = bodyAppliedData
                )

                onlineRecommendations = outfits.take(5)
            } catch (e: Exception) {
                Log.e("AssistanceScreen", "OutfitFetcher error: ${e.message}")
                onlineRecommendations = emptyList()
            }
            step = "results"
        }
    }

    // ==================== Layout ====================
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
                    onGenerate = { handleGenerateRecommendation() },
                    bodyAppliedData = bodyAppliedData
                )
                "generating" -> GeneratingStep()
                "results" -> ResultsStep(
                    formData = formData,
                    onNavigate = onNavigate,
                    onTryAgain = { step = "input" },
                    onlineRecommendations = onlineRecommendations
                )
            }
        }
    }
}

// ==================== Body Data Box ====================
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

// ==================== Temperature Picker ====================
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

// ==================== Generating Step ====================
@Composable
fun GeneratingStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        CircularProgressIndicator(color = Color(0xFF8B5CF6))
        Text("Creating Your Perfect Look", style = MaterialTheme.typography.titleLarge)
        Text("Analyzing your style preferences and wardrobe...")
    }
}

// ==================== Results Step ====================
// ==================== Results Step ====================
@Composable
fun ResultsStep(
    formData: AssistantForm,
    onNavigate: (String) -> Unit,
    onTryAgain: () -> Unit,
    onlineRecommendations: List<OnlineOutfit>
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Your ${formData.theme} outfit for ${formData.event}",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Weather: ${formData.weather}, Temperature: ${formData.temperature}°C",
            color = Color.Gray
        )

        if (onlineRecommendations.isEmpty()) {
            Text(
                "No outfit results found for your current preferences. Try changing the event, theme, or weather!",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            onlineRecommendations.forEach { outfit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(outfit.productUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("ResultsStep", "Error opening link: ${e.message}")
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        AsyncImage(
                            model = outfit.imageUrl,
                            contentDescription = outfit.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            outfit.title,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF111827)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap to view product →",
                            color = Color(0xFF3B82F6),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onTryAgain,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Try Again", color = Color.White)
        }
    }
}

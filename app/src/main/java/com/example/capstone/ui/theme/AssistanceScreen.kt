package com.example.capstone.ui.theme

import android.content.Intent
import android.net.Uri
import android.util.Log
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
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
    val venueCondition: String
)

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
                venueCondition = ""
            )
        )
    }

    val scope = rememberCoroutineScope()

    val eventTypes = listOf(
        "Casual outing", "Work meeting", "Date night", "Party", "Wedding",
        "Gym/Sports", "Beach/Pool", "Shopping", "Concert", "Dinner"
    )

    // Smart theme ranking based on event type
    val eventThemeRankings = mapOf(
        "Casual outing" to listOf("Casual", "Minimalist", "Trendy", "Bohemian", "Edgy", "Chic", "Sporty", "Classic", "Professional", "Formal"),
        "Work meeting" to listOf("Professional", "Classic", "Minimalist", "Formal", "Chic", "Casual", "Trendy", "Edgy", "Bohemian", "Sporty"),
        "Date night" to listOf("Chic", "Trendy", "Edgy", "Classic", "Formal", "Casual", "Minimalist", "Bohemian", "Professional", "Sporty"),
        "Party" to listOf("Trendy", "Edgy", "Chic", "Bohemian", "Casual", "Classic", "Formal", "Minimalist", "Professional", "Sporty"),
        "Wedding" to listOf("Formal", "Chic", "Classic", "Trendy", "Professional", "Minimalist", "Casual", "Edgy", "Bohemian", "Sporty"),
        "Gym/Sports" to listOf("Sporty", "Casual", "Minimalist", "Trendy", "Edgy", "Bohemian", "Chic", "Classic", "Professional", "Formal"),
        "Beach/Pool" to listOf("Casual", "Bohemian", "Trendy", "Sporty", "Minimalist", "Chic", "Edgy", "Classic", "Professional", "Formal"),
        "Shopping" to listOf("Casual", "Trendy", "Chic", "Minimalist", "Edgy", "Bohemian", "Classic", "Sporty", "Professional", "Formal"),
        "Concert" to listOf("Edgy", "Trendy", "Casual", "Bohemian", "Chic", "Sporty", "Classic", "Minimalist", "Professional", "Formal"),
        "Dinner" to listOf("Chic", "Classic", "Formal", "Trendy", "Casual", "Minimalist", "Professional", "Edgy", "Bohemian", "Sporty")
    )

    val weatherConditions = listOf(
        "Sunny", "Cloudy", "Rainy", "Windy", "Humid", "Hot", "Cool", "Stormy"
    )

    val venueConditions = listOf(
        "Outdoor - Hot", "Outdoor - Cool", "Indoor - Air Conditioned",
        "Indoor - Heated", "Indoor - No Climate Control", "Mixed (Indoor/Outdoor)"
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
        if (formData.event.isBlank() || formData.theme.isBlank() ||
            formData.weather.isBlank() || formData.venueCondition.isBlank()) return

        step = "generating"
        scope.launch {
            try {
                val outfits = OutfitFetcher.getOutfitRecommendations(
                    eventType = formData.event,
                    preferredStyle = formData.theme,
                    theme = formData.theme,
                    currentWeather = formData.weather,
                    temperature = 25.0,
                    bodyData = bodyAppliedData
                )

                onlineRecommendations = outfits
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
                    eventThemeRankings = eventThemeRankings,
                    weatherConditions = weatherConditions,
                    venueConditions = venueConditions,
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

// ==================== Input Step ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputStep(
    formData: AssistantForm,
    onFormChange: (AssistantForm) -> Unit,
    eventTypes: List<String>,
    eventThemeRankings: Map<String, List<String>>,
    weatherConditions: List<String>,
    venueConditions: List<String>,
    onGenerate: () -> Unit,
    bodyAppliedData: Map<String, String>
) {
    var selectedEvent by remember { mutableStateOf(formData.event) }
    var selectedTheme by remember { mutableStateOf(formData.theme) }
    var selectedWeather by remember { mutableStateOf(formData.weather) }
    var selectedVenue by remember { mutableStateOf(formData.venueCondition) }

    // Get ranked themes based on selected event
    val rankedThemes = if (selectedEvent.isNotEmpty()) {
        eventThemeRankings[selectedEvent] ?: eventThemeRankings.values.first()
    } else {
        eventThemeRankings.values.first()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Style Assistant",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text("Tell me about your occasion and I'll create the perfect outfit")

        // Event Type Selection
        Text("Event Type", fontWeight = FontWeight.Medium)
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // Theme Selection (Smart Ranked)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Preferred Style Theme", fontWeight = FontWeight.Medium)
            if (selectedEvent.isNotEmpty()) {
                Text(
                    "★ Ranked for ${selectedEvent}",
                    fontSize = 11.sp,
                    color = Color(0xFF8B5CF6),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (row in rankedThemes.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        val rankIndex = rankedThemes.indexOf(theme)
                        val isBestMatch = rankIndex < 3 && selectedEvent.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF8B5CF6)
                                    else if (isBestMatch) Color(0xFFEDE9FE)
                                    else Color(0xFFF3F4F6),
                                    RoundedCornerShape(50)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF8B5CF6)
                                    else if (isBestMatch) Color(0xFFA78BFA)
                                    else Color(0xFF9CA3AF),
                                    RoundedCornerShape(50)
                                )
                                .padding(vertical = 10.dp)
                                .clickable {
                                    selectedTheme = theme
                                    onFormChange(formData.copy(theme = theme))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isBestMatch && !isSelected) {
                                    Text(
                                        "★",
                                        color = Color(0xFF8B5CF6),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                Text(
                                    text = theme,
                                    color = if (isSelected) Color.White
                                    else if (isBestMatch) Color(0xFF6D28D9)
                                    else Color.Black,
                                    fontWeight = if (isSelected || isBestMatch) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // Weather Selection
        Text("Current Weather", fontWeight = FontWeight.Medium)
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
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }

        // Venue Condition Selection
        Text("Venue Condition", fontWeight = FontWeight.Medium)
        Text(
            "Consider where you'll spend most time",
            fontSize = 12.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            venueConditions.forEach { venue ->
                val isSelected = selectedVenue == venue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Color(0xFF8B5CF6) else Color(0xFFF3F4F6),
                            RoundedCornerShape(12)
                        )
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF8B5CF6) else Color(0xFF9CA3AF),
                            RoundedCornerShape(12)
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                        .clickable {
                            selectedVenue = venue
                            onFormChange(formData.copy(venueCondition = venue))
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = venue,
                            color = if (isSelected) Color.White else Color.Black,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

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
                Text(label, color = Color(0xFF4B5563), fontSize = 12.sp)
                Text(value, fontWeight = FontWeight.Medium, fontSize = 12.sp)
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

// ==================== Generating Step ====================
@Composable
fun GeneratingStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        CircularProgressIndicator(color = Color(0xFF8B5CF6), modifier = Modifier.size(64.dp))
        Text("Creating Your Perfect Look", style = MaterialTheme.typography.titleLarge)
        Text("Analyzing your style preferences and wardrobe...", color = Color.Gray)
    }
}

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
            "Weather: ${formData.weather} | Venue: ${formData.venueCondition}",
            color = Color.Gray,
            fontSize = 13.sp
        )

        if (onlineRecommendations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No items found. Try different preferences!",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            onlineRecommendations.forEach { outfit ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                if (outfit.productUrl != "#") {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(outfit.productUrl))
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                Log.e("ResultsStep", "Error opening link: ${e.message}")
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Image Container with State Management
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(outfit.imageUrl)
                                    .crossfade(300)
                                    .build(),
                                contentDescription = outfit.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = Color(0xFF8B5CF6)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text("Loading...", fontSize = 14.sp, color = Color.Gray)
                                    }
                                },
                                error = {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFFFEBEE))
                                            .padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.BrokenImage,
                                            contentDescription = null,
                                            tint = Color(0xFFE53935),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Image unavailable",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFE53935)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "Tap to view on website",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            outfit.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF1F2937),
                            maxLines = 2,
                            lineHeight = 22.sp
                        )

                        if (outfit.price.isNotEmpty() && outfit.category != "error") {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                outfit.price,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color(0xFF10B981)
                            )
                        }

                        if (outfit.productUrl != "#") {
                            Spacer(Modifier.height(12.dp))

                            Surface(
                                color = Color(0xFFEFF6FF),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Tap to view on website",
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onTryAgain,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Try Different Preferences", color = Color.White)
            }
        }
    }
}
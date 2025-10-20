

package com.example.capstone.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.capstone.missions.MissionManager
import com.example.capstone.missions.UserProgress

data class BodyTypeInfo(
    val description: String,
    val tips: List<String>,
    val emoji: String
)

@Composable
fun ProfileScreen(
    onNavigate: (String) -> Unit,
    user: User,
    bodyMeasurements: Map<String, String>,
    bodyRatios: Map<String, String>
) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    var isLoading by remember { mutableStateOf(true) }
    var fullUserData by remember { mutableStateOf(user) }
    var measurements by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var ratios by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // FIX: Use snapshot listener for real-time updates
    LaunchedEffect(uid) {
        if (uid != null) {
            val userDoc = db.collection("users").document(uid)

            // Real-time listener
            userDoc.addSnapshotListener { userSnap, error ->
                if (error != null) {
                    android.util.Log.e("ProfileScreen", "Error listening", error)
                    isLoading = false
                    return@addSnapshotListener
                }

                if (userSnap != null && userSnap.exists()) {
                    val userData = userSnap.data ?: emptyMap<String, Any>()
                    fullUserData = user.copy(
                        name = userData["name"]?.toString() ?: user.name,
                        email = userData["email"]?.toString() ?: user.email,
                        height = userData["height"]?.toString() ?: "N/A",
                        weight = userData["weight"]?.toString() ?: "N/A",
                        age = userData["age"]?.toString() ?: "N/A",
                        gender = userData["gender"]?.toString() ?: "Not Set"
                    )
                }

                // Load bodyComposition
                userDoc.collection("bodyComposition").document("latest")
                    .get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val data = doc.data ?: emptyMap<String, Any>()
                            val tempMeasurements = mutableMapOf<String, String>()
                            val tempRatios = mutableMapOf<String, String>()

                            data.forEach { (key, value) ->
                                if (key.contains("Ratio", ignoreCase = true)) {
                                    tempRatios[key] = value.toString()
                                } else if (key != "timestamp") {
                                    tempMeasurements[key] = value.toString()
                                }
                            }

                            measurements = tempMeasurements
                            ratios = tempRatios
                        }
                        isLoading = false
                    }
                    .addOnFailureListener { isLoading = false }
            }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        ProfileContent(
            onNavigate = onNavigate,
            user = fullUserData,
            bodyMeasurements = if (measurements.isNotEmpty()) measurements else bodyMeasurements,
            bodyRatios = if (ratios.isNotEmpty()) ratios else bodyRatios
        )
    }
}

@Composable
fun ProfileContent(
    onNavigate: (String) -> Unit,
    user: User,
    bodyMeasurements: Map<String, String>,
    bodyRatios: Map<String, String>
) {
    var useInches by rememberSaveable { mutableStateOf(false) }
    var showPercentages by rememberSaveable { mutableStateOf(false) }

    val bodyTypeInfo = mapOf(
        "Rectangle" to BodyTypeInfo(
            description = "Well-balanced proportions with similar bust, waist, and hip measurements",
            tips = listOf(
                "Create curves with fitted tops and flare bottoms",
                "Use belts to define your waist",
                "Layer different textures for visual interest",
                "Try wrap dresses and peplum tops"
            ),
            emoji = "👤"
        )
    )

    val info = bodyTypeInfo[user.bodyType] ?: bodyTypeInfo["Rectangle"]!!

    // Load user progress for level display
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    val missionManager = remember {
        if (userId != null) MissionManager(userId, db) else null
    }
    var userProgress by remember { mutableStateOf(UserProgress()) }

    // FIX: Use snapshot listener here too
    LaunchedEffect(userId) {
        if (userId != null && missionManager != null) {
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("ProfileContent", "Error listening", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        userProgress = UserProgress(
                            level = (snapshot.getLong("level")?.toInt() ?: 1).coerceIn(1, 20),
                            currentXP = snapshot.getLong("currentXP")?.toInt() ?: 0,
                            totalXP = snapshot.getLong("totalXP")?.toInt() ?: 0,
                            maxStorage = snapshot.getLong("maxStorage")?.toInt() ?: 20,
                            currentStorage = snapshot.getLong("currentStorage")?.toInt() ?: 0
                        )
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9FAFB))
            .padding(bottom = 16.dp)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF0D9488))
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onNavigate("home") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text("Profile", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = { /* Settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }

        // Replace the entire "User Info Card WITH Level & Progress at TOP" section
// in ProfileContent (around lines 140-230)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            user.name.firstOrNull()?.toString() ?: "U",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(user.email, fontSize = 14.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))

                        // Level Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Level ${userProgress.level}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                "${userProgress.totalXP} Total XP",
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Digital Closet Card - NEW DESIGN MATCHING RIGHT IMAGE
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFD1FAE5)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Checkroom,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Digital Closet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }

                            Text(
                                text = "${userProgress.currentStorage}/${userProgress.maxStorage}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Unlock more space by doing missions",
                            fontSize = 13.sp,
                            color = Color(0xFF065F46).copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Prominent View Missions Button
                        Button(
                            onClick = { onNavigate("missions") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor  = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "View Missions",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // XP Progress Bar (below Digital Closet)
                if (missionManager != null) {
                    val xpNeeded = missionManager.getXPNeededForLevel(userProgress.level)
                    val progress = if (userProgress.level >= 20) 1f
                    else userProgress.currentXP.toFloat() / xpNeeded

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Level Progress",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Text(
                                "${userProgress.currentXP}/$xpNeeded XP",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF10B981),
                            backgroundColor = Color(0xFFE5E7EB)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Next level info
                        if (userProgress.level < 20) {
                            Text(
                                "${xpNeeded - userProgress.currentXP} XP to Level ${userProgress.level + 1}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                "Max Level Reached! 🎉",
                                fontSize = 12.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row (Outfits, Items, Challenge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBox("0", "Outfits", Color(0xFF10B981), Modifier.weight(1f))
                    StatBox("0", "Items", Color(0xFF2563EB), Modifier.weight(1f))
                    StatBox("0%", "Challenge", Color(0xFF7C3AED), Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Body Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Body Analysis", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { onNavigate("scan") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rescan", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD1FAE5), shape = RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(info.emoji, fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(user.bodyType, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF065F46))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(info.description, fontSize = 14.sp, color = Color(0xFF065F46))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("${user.height} in", "Height", Modifier.weight(1f))
                    StatCard("${user.weight} kg", "Weight", Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(user.age, "Age", Modifier.weight(1f))
                    StatCard(user.gender, "Gender", Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        BodyMeasurementsSection(bodyMeasurements, useInches)
        Spacer(modifier = Modifier.height(12.dp))
        BodyRatiosSection(bodyRatios, showPercentages)

        // FIX: Removed duplicate leveling section from bottom - it's now at the top!
    }
}

@Composable
fun StatBox(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(4.dp)) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Text(value, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun BodyMeasurementsSection(bodyMeasurements: Map<String, String>, useInches: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        var useInch by rememberSaveable { mutableStateOf(useInches) }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Body Measurements", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("cm", color = if (!useInch) Color(0xFF10B981) else Color.Gray)
                    Switch(checked = useInch, onCheckedChange = { useInch = it })
                    Text("in", color = if (useInch) Color(0xFF10B981) else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val defaultMeasurements = listOf(
                "Shoulder Width",
                "Hip Width",
                "Torso Length",
                "Leg Length",
                "Arm Length",
                "Estimated Skin Color"
            )

            defaultMeasurements.forEach { label ->
                val value = bodyMeasurements[label] ?: "N/A"
                val displayValue = if (value != "N/A" && label != "Estimated Skin Color") {
                    val num = value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                    if (num != null) {
                        if (useInch) String.format("%.2f in", num / 2.54)
                        else String.format("%.2f cm", num)
                    } else value
                } else value
                InfoRow(label, displayValue)
            }
        }
    }
}

@Composable
private fun BodyRatiosSection(bodyRatios: Map<String, String>, showPercentages: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        var showPct by rememberSaveable { mutableStateOf(showPercentages) }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Body Ratios", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ratio", color = if (!showPct) Color(0xFF10B981) else Color.Gray)
                    Switch(checked = showPct, onCheckedChange = { showPct = it })
                    Text("%", color = if (showPct) Color(0xFF10B981) else Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val defaultRatios = listOf(
                "Shoulder-to-Hip Ratio",
                "Torso-to-Leg Ratio",
                "Arm-to-Leg Ratio",
                "Shoulder-to-Height Ratio"
            )

            defaultRatios.forEach { label ->
                val value = bodyRatios[label] ?: "N/A"
                val displayValue = if (value != "N/A") {
                    val num = value.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                    if (num != null) {
                        if (showPct) String.format("%.1f%%", num * 100)
                        else String.format("%.3f", num)
                    } else value
                } else value
                InfoRow(label, displayValue)
            }
        }
    }
}
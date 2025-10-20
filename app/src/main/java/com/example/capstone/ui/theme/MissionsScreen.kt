package com.example.capstone.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.capstone.missions.Mission
import com.example.capstone.missions.MissionManager
import com.example.capstone.missions.UserProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsScreen(navController: NavController) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    // FIX: Handle null userId gracefully
    if (userId == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please log in to view missions")
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    // FIX: Safe navigation - check if login route exists
                    try {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MissionsScreen", "Navigation error", e)
                    }
                }) {
                    Text("Go to Login")
                }
            }
        }
        return
    }

    val db = FirebaseFirestore.getInstance()
    val missionManager = remember { MissionManager(userId, db) }
    val scope = rememberCoroutineScope()

    var userProgress by remember { mutableStateOf(UserProgress()) }
    var missions by remember { mutableStateOf<List<Mission>>(emptyList()) }
    var timeRemaining by remember { mutableStateOf(missionManager.getTimeUntilMidnight()) }
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var newLevel by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // FIX: Load data with better error handling
    LaunchedEffect(Unit) {
        try {
            userProgress = missionManager.getUserProgress()
            missions = missionManager.getTodaysMissions()
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Error loading missions: ${e.message}"
            isLoading = false
            android.util.Log.e("MissionsScreen", "Load error", e)
        }

        // Countdown timer
        while (true) {
            delay(1000)
            timeRemaining = missionManager.getTimeUntilMidnight()
        }
    }

    // Show level up dialog
    if (showLevelUpDialog) {
        LevelUpDialog(
            newLevel = newLevel,
            onDismiss = { showLevelUpDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Missions") },
                navigationIcon = {
                    IconButton(onClick = {
                        // FIX: Improved navigation with multiple fallback options
                        try {
                            // Try to pop back stack first
                            if (!navController.popBackStack()) {
                                // If pop fails, navigate to home explicitly
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MissionsScreen", "Navigation error", e)
                            // Last resort: try direct home navigation
                            try {
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = false }
                                    launchSingleTop = true
                                }
                            } catch (e2: Exception) {
                                android.util.Log.e("MissionsScreen", "Critical nav error", e2)
                            }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF10B981),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "Unknown error",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                scope.launch {
                                    try {
                                        userProgress = missionManager.getUserProgress()
                                        missions = missionManager.getTodaysMissions()
                                        isLoading = false
                                    } catch (e: Exception) {
                                        errorMessage = "Error: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF9FAFB))
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Level Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Level badge
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Text(
                                            "Level ${userProgress.level}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // XP Progress
                                val xpNeeded = missionManager.getXPNeededForLevel(userProgress.level)
                                val progress = if (userProgress.level >= 20) 1f
                                else userProgress.currentXP.toFloat() / xpNeeded

                                Text(
                                    "${userProgress.currentXP}/$xpNeeded XP",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    color = Color(0xFF10B981),
                                    trackColor = Color(0xFFE5E7EB)
                                )

                                Spacer(Modifier.height(8.dp))

                                if (userProgress.level < 20) {
                                    Text(
                                        "${xpNeeded - userProgress.currentXP} XP to Level ${userProgress.level + 1}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                } else {
                                    Text(
                                        "Max Level Reached!",
                                        fontSize = 12.sp,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    "Level up to unlock more Clothes and Creation Capacity",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }

                    // Today's Mission Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Today's Mission",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                missionManager.formatTimeRemaining(timeRemaining),
                                fontSize = 14.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Mission List
                    items(missions) { mission ->
                        MissionCard(
                            mission = mission,
                            onClaim = {
                                scope.launch {
                                    try {
                                        val result = missionManager.claimMission(mission.id, mission.xpReward)

                                        // Refresh missions safely
                                        missions = missionManager.getTodaysMissions()
                                        userProgress = missionManager.getUserProgress()

                                        // Show level up dialog
                                        if (result.leveledUp) {
                                            newLevel = result.newLevel
                                            showLevelUpDialog = true
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MissionsScreen", "Claim error", e)
                                        errorMessage = "Failed to claim: ${e.message}"
                                    }
                                }
                            },
                            onGo = { missionId ->
                                try {
                                    when (missionId) {
                                        "shareApp" -> {
                                            val shareIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Check out FitMatch - Your Personal Style Assistant! 👔✨"
                                                )
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share FitMatch"))

                                            scope.launch {
                                                try {
                                                    missionManager.completeMission("shareApp")
                                                    missions = missionManager.getTodaysMissions()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("MissionsScreen", "Complete error", e)
                                                }
                                            }
                                        }
                                        "watchAd30s" -> {
                                            navController.navigate("ad/30")
                                        }
                                        "watchAd1min" -> {
                                            navController.navigate("ad/60")
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MissionsScreen", "Mission action error", e)
                                    errorMessage = "Action failed: ${e.message}"
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MissionCard(
    mission: Mission,
    onClaim: () -> Unit,
    onGo: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mission.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${mission.xpReward} XP",
                    fontSize = 13.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.SemiBold
                )
            }

            when {
                mission.claimed -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CLAIMED")
                    }
                }
                mission.completed -> {
                    Button(
                        onClick = onClaim,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CLAIM", color = Color.White)
                    }
                }
                else -> {
                    Button(
                        onClick = { onGo(mission.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B7280)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GO", color = Color.White)
                    }
                }
            }
        }
    }
}
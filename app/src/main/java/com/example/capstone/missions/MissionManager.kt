package com.example.capstone.missions

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class UserProgress(
    val level: Int = 1,
    val currentXP: Int = 0,
    val totalXP: Int = 0,
    val maxStorage: Int = 20,
    val currentStorage: Int = 0
)

data class Mission(
    val id: String,
    val title: String,
    val xpReward: Int,
    val completed: Boolean = false,
    val claimed: Boolean = false
)

data class LevelUpResult(
    val leveledUp: Boolean,
    val newLevel: Int,
    val previousLevel: Int
)

class MissionManager(
    private val userId: String,
    private val db: FirebaseFirestore
) {

    // Calculate XP needed for a specific level
    fun getXPNeededForLevel(level: Int): Int {
        return when {
            level <= 10 -> 100
            level <= 20 -> 200
            else -> 300 + ((level - 20) * 100) // Continues scaling after 20
        }
    }

    // Calculate max storage based on level
    fun getMaxStorageForLevel(level: Int): Int {
        val baseStorage = 20 // Level 1 starts with 20
        return baseStorage + ((level - 1) * 5) // +5 per level
    }

    // Get current date string for mission tracking
    fun getCurrentDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    // Load user progress
    suspend fun getUserProgress(): UserProgress {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            UserProgress(
                level = (doc.getLong("level")?.toInt() ?: 1).coerceIn(1, 20),
                currentXP = doc.getLong("currentXP")?.toInt() ?: 0,
                totalXP = doc.getLong("totalXP")?.toInt() ?: 0,
                maxStorage = doc.getLong("maxStorage")?.toInt() ?: 20,
                currentStorage = doc.getLong("currentStorage")?.toInt() ?: 0
            )
        } catch (e: Exception) {
            println("Error loading user progress: ${e.message}")
            UserProgress()
        }
    }

    // Load today's missions
    suspend fun getTodaysMissions(): List<Mission> {
        val today = getCurrentDateString()
        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("missions")
                .document(today)
                .get()
                .await()

            if (!doc.exists()) {
                // Initialize today's missions
                initializeTodaysMissions()
                return getDefaultMissions()
            }

            val data = doc.data ?: return getDefaultMissions()

            listOf(
                Mission(
                    "dailyLogin",
                    "Daily Login",
                    15,
                    data["dailyLogin_completed"] as? Boolean ?: false,
                    data["dailyLogin_claimed"] as? Boolean ?: false
                ),
                Mission(
                    "shareApp",
                    "Share This App",
                    15,
                    data["shareApp_completed"] as? Boolean ?: false,
                    data["shareApp_claimed"] as? Boolean ?: false
                ),
                Mission(
                    "watchAd30s",
                    "Watch 30s Ad",
                    15,
                    data["watchAd30s_completed"] as? Boolean ?: false,
                    data["watchAd30s_claimed"] as? Boolean ?: false
                ),
                Mission(
                    "watchAd1min",
                    "Watch 1 Minute Ad",
                    15,
                    data["watchAd1min_completed"] as? Boolean ?: false,
                    data["watchAd1min_claimed"] as? Boolean ?: false
                ),
                Mission(
                    "finish4Missions",
                    "Finish 4 Mission Today",
                    25,
                    data["finish4Missions_completed"] as? Boolean ?: false,
                    data["finish4Missions_claimed"] as? Boolean ?: false
                )
            )
        } catch (e: Exception) {
            println("Error loading missions: ${e.message}")
            getDefaultMissions()
        }
    }

    private fun getDefaultMissions() = listOf(
        Mission("dailyLogin", "Daily Login", 15),
        Mission("shareApp", "Share This App", 15),
        Mission("watchAd30s", "Watch 30s Ad", 15),
        Mission("watchAd1min", "Watch 1 Minute Ad", 15),
        Mission("finish4Missions", "Finish 4 Mission Today", 25)
    )

    // Initialize today's missions in Firestore
    private suspend fun initializeTodaysMissions() {
        val today = getCurrentDateString()
        try {
            db.collection("users")
                .document(userId)
                .collection("missions")
                .document(today)
                .set(mapOf(
                    "date" to today,
                    "dailyLogin_completed" to true, // Auto-complete on login
                    "dailyLogin_claimed" to false,
                    "shareApp_completed" to false,
                    "shareApp_claimed" to false,
                    "watchAd30s_completed" to false,
                    "watchAd30s_claimed" to false,
                    "watchAd1min_completed" to false,
                    "watchAd1min_claimed" to false,
                    "finish4Missions_completed" to false,
                    "finish4Missions_claimed" to false,
                    "timestamp" to Timestamp.now()
                ))
                .await()
        } catch (e: Exception) {
            println("Error initializing missions: ${e.message}")
        }
    }

    // Mark mission as completed
    suspend fun completeMission(missionId: String) {
        val today = getCurrentDateString()
        try {
            db.collection("users")
                .document(userId)
                .collection("missions")
                .document(today)
                .update("${missionId}_completed", true)
                .await()

            // Check if "Finish 4 Missions" should be completed
            checkFinish4Missions()
        } catch (e: Exception) {
            println("Error completing mission: ${e.message}")
        }
    }

    // Check if 4 missions are claimed
    private suspend fun checkFinish4Missions() {
        val missions = getTodaysMissions()
        val claimedCount = missions.filter {
            it.id != "finish4Missions" && it.claimed
        }.size

        if (claimedCount >= 4) {
            completeMission("finish4Missions")
        }
    }

    // Claim mission reward
    suspend fun claimMission(missionId: String, xpReward: Int): LevelUpResult {
        val today = getCurrentDateString()
        try {
            // Mark as claimed
            db.collection("users")
                .document(userId)
                .collection("missions")
                .document(today)
                .update("${missionId}_claimed", true)
                .await()

            // Add XP
            val result = addXP(xpReward)

            // Check finish4Missions completion
            checkFinish4Missions()

            return result
        } catch (e: Exception) {
            println("Error claiming mission: ${e.message}")
            return LevelUpResult(false, 1, 1)
        }
    }

    // Add XP and check for level up
    suspend fun addXP(amount: Int): LevelUpResult {
        try {
            val progress = getUserProgress()
            var newCurrentXP = progress.currentXP + amount
            val newTotalXP = progress.totalXP + amount
            var newLevel = progress.level
            var leveledUp = false

            // Check if level up (only +1 level per claim)
            val xpNeeded = getXPNeededForLevel(newLevel)
            if (newCurrentXP >= xpNeeded && newLevel < 20) {
                newCurrentXP -= xpNeeded
                newLevel += 1
                leveledUp = true
            }

            val newMaxStorage = getMaxStorageForLevel(newLevel)

            // Update Firestore
            db.collection("users").document(userId).update(
                mapOf(
                    "level" to newLevel,
                    "currentXP" to newCurrentXP,
                    "totalXP" to newTotalXP,
                    "maxStorage" to newMaxStorage
                )
            ).await()

            return LevelUpResult(
                leveledUp = leveledUp,
                newLevel = newLevel,
                previousLevel = progress.level
            )
        } catch (e: Exception) {
            println("Error adding XP: ${e.message}")
            return LevelUpResult(false, 1, 1)
        }
    }

    // Get time until midnight for countdown
    fun getTimeUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return midnight.timeInMillis - now.timeInMillis
    }

    // Format milliseconds to HH:MM:SS
    fun formatTimeRemaining(millis: Long): String {
        val hours = (millis / (1000 * 60 * 60)) % 24
        val minutes = (millis / (1000 * 60)) % 60
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
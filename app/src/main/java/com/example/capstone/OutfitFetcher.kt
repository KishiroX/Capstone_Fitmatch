package com.example.capstone

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

data class OnlineOutfit(
    val title: String,
    val imageUrl: String,
    val productUrl: String
)

object OutfitFetcher {
    private const val TAG = "OutfitFetcher"

    // 🔑 Your Lykdat API key
    private const val LYKDAT_API_KEY =
        "5fd66fe6dd496bddacbb205d85f12ac2c00881de51807af2542ab25183c2816b"

    // --------------------------------------------------------------------------
    // 🔹 Fetch Outfit Data from Lykdat API
    // --------------------------------------------------------------------------
    suspend fun fetchFromLykdat(imageUrl: String): List<OnlineOutfit> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineOutfit>()

        try {
            val url = "https://api.lykdat.com/v2/detect-item"

            // Request JSON body
            val jsonBody = """
                {
                    "image_url": "$imageUrl"
                }
            """.trimIndent()

            val client = OkHttpClient()
            val mediaType = "application/json".toMediaTypeOrNull()
            val body = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $LYKDAT_API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "✅ Lykdat response: $responseBody")

                // Parse detected items (simplified)
                val json = JSONObject(responseBody ?: "")
                val dataArray = json.optJSONArray("data")

                if (dataArray != null) {
                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val productName = item.optString("name", "Unnamed Outfit")
                        val image = item.optString("image", "")
                        val link = item.optString("link", "")

                        results.add(
                            OnlineOutfit(
                                title = productName,
                                imageUrl = image.ifEmpty { "https://via.placeholder.com/150" },
                                productUrl = link.ifEmpty { "#" }
                            )
                        )
                    }
                }

            } else {
                Log.e(TAG, "❌ Lykdat API Error: ${response.code} ${response.message}")
                Log.e(TAG, "Response: ${response.body?.string()}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error fetching from Lykdat: ${e.message}")
        }

        if (results.isEmpty()) {
            Log.d(TAG, "⚠️ No results found from Lykdat")
        }

        results
    }

    // --------------------------------------------------------------------------
    // 🔹 Generate Outfit Recommendations
    // --------------------------------------------------------------------------
    suspend fun getOutfitRecommendations(
        eventType: String,
        preferredStyle: String,
        theme: String,
        currentWeather: String,
        temperature: Double,
        bodyData: Map<String, String>
    ): List<OnlineOutfit> = withContext(Dispatchers.IO) {

        val gender = bodyData["Gender"] ?: "unisex"

        val tempClothing = when {
            temperature > 28 -> "light summer clothes"
            temperature < 20 -> "warm layered outfit"
            else -> "comfortable casual wear"
        }

        val styleKeywordMap = mapOf(
            "Edgy" to listOf("leather", "ripped", "black", "denim"),
            "Sporty" to listOf("activewear", "dry-fit", "track pants"),
            "Casual" to listOf("t-shirt", "jeans", "hoodie"),
            "Formal" to listOf("blazer", "slacks", "dress shirt"),
            "Chic" to listOf("silk", "elegant", "minimalist"),
            "Bohemian" to listOf("flowy", "earth tones", "patterned"),
            "Professional" to listOf("button-down", "trousers", "neutral colors")
        )

        val eventKeywordMap = mapOf(
            "Beach/Pool" to listOf("swimwear", "tank top", "linen", "vacation set"),
            "Gym/Sports" to listOf("activewear", "training shirt", "compression"),
            "Wedding" to listOf("gown", "barong", "formal dress", "suit"),
            "Work meeting" to listOf("blazer", "slacks", "business casual"),
            "Casual outing" to listOf("t-shirt", "jeans", "hoodie"),
            "Party" to listOf("dress", "jumpsuit", "stylish top"),
            "Date night" to listOf("chic", "romantic", "elegant"),
            "Concert" to listOf("graphic tee", "denim", "boots"),
            "Shopping" to listOf("comfortable", "casual"),
            "Dinner" to listOf("smart casual", "collared shirt", "dressy top")
        )

        val styleKeywords = styleKeywordMap[preferredStyle]?.joinToString(" ") ?: ""
        val eventKeywords = eventKeywordMap[eventType]?.joinToString(" ") ?: ""

        val query = "$preferredStyle $styleKeywords $eventKeywords $tempClothing outfit for $gender"
        Log.d(TAG, "👗 Generated recommendation query: $query")

        // 🔹 Instead of Google, fetch using Lykdat (you can later plug in real image URL)
        val testImageUrl = "https://cdn.shopify.com/s/files/1/0604/4803/collections/men-outfit.jpg"
        val lykdatResults = fetchFromLykdat(testImageUrl)

        return@withContext if (lykdatResults.isNotEmpty()) {
            lykdatResults
        } else {
            Log.d(TAG, "⚠️ No Lykdat results, returning fallback recommendation")
            listOf(
                OnlineOutfit(
                    title = "Casual Outfit Suggestion",
                    imageUrl = "https://via.placeholder.com/150",
                    productUrl = "#"
                )
            )
        }
    }
}

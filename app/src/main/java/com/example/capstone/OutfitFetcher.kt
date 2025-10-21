package com.example.capstone

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OnlineOutfit(
    val title: String,
    val imageUrl: String,
    val productUrl: String,
    val category: String = "",
    val price: String = ""
)

object OutfitFetcher {
    private const val TAG = "OutfitFetcher"
    private const val RAPIDAPI_KEY = "f1cd3ae49emsh2716b05b3308faep1211d9jsn6fc015afd107"
    private const val RAPIDAPI_HOST = "asos2.p.rapidapi.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Main entry point for getting outfit recommendations
     */
    suspend fun getOutfitRecommendations(
        eventType: String,
        preferredStyle: String,
        theme: String,
        currentWeather: String,
        temperature: Double,
        bodyData: Map<String, String>
    ): List<OnlineOutfit> = withContext(Dispatchers.IO) {

        Log.d(TAG, "========================================")
        Log.d(TAG, "🎯 Starting Outfit Recommendations")
        Log.d(TAG, "Event: $eventType")
        Log.d(TAG, "Style: $preferredStyle")
        Log.d(TAG, "Weather: $currentWeather")
        Log.d(TAG, "Temperature: ${temperature}°C")
        Log.d(TAG, "========================================")

        val gender = determineGender(bodyData)
        val queries = generateSearchQueries(eventType, preferredStyle, temperature, gender)

        Log.d(TAG, "🔍 Generated ${queries.size} search queries:")
        queries.forEachIndexed { index, query ->
            Log.d(TAG, "  [$index] $query")
        }

        // Execute all searches in parallel
        val allResults = queries.map { query ->
            async {
                try {
                    searchASOS(query, gender)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Search failed for '$query': ${e.message}", e)
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        Log.d(TAG, "📦 Total results before filtering: ${allResults.size}")

        // Remove duplicates and limit results
        val uniqueResults = allResults
            .distinctBy { it.imageUrl }
            .filter { it.category != "error" }
            .take(10)

        Log.d(TAG, "✅ Final results: ${uniqueResults.size} items")

        if (uniqueResults.isEmpty()) {
            Log.w(TAG, "⚠️ No results found, returning placeholder")
            return@withContext listOf(createErrorOutfit())
        }

        uniqueResults.forEachIndexed { index, outfit ->
            Log.d(TAG, "[$index] ${outfit.title}")
            Log.d(TAG, "     💰 ${outfit.price}")
            Log.d(TAG, "     🖼️ ${outfit.imageUrl.take(80)}...")
        }

        return@withContext uniqueResults
    }

    /**
     * Search ASOS API with a specific query (v2 endpoint)
     */
    private suspend fun searchASOS(query: String, gender: String): List<OnlineOutfit> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineOutfit>()

        try {
            Log.d(TAG, "🌐 Searching ASOS: '$query'")

            // Encode query properly
            val encodedQuery = query.replace(" ", "%20")
            val url = buildString {
                append("https://asos2.p.rapidapi.com/products/v2/list")
                append("?store=US")
                append("&offset=0")
                append("&limit=20")
                append("&country=US")
                append("&sort=freshness")
                append("&q=$encodedQuery")
                append("&lang=en-US")
                append("&sizeSchema=US")
                append("&currency=USD")
            }

            Log.d(TAG, "📡 URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-host", RAPIDAPI_HOST)
                .addHeader("x-rapidapi-key", RAPIDAPI_KEY)
                .build()

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()

            Log.d(TAG, "📥 Response Code: $responseCode")
            Log.d(TAG, "📥 Response Length: ${responseBody?.length ?: 0} characters")

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ API Error $responseCode: ${response.message}")
                Log.e(TAG, "Response Body: ${responseBody?.take(500)}")
                return@withContext emptyList()
            }

            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "❌ Empty response body")
                return@withContext emptyList()
            }

            Log.d(TAG, "📄 Response preview: ${responseBody.take(500)}")

            val json = JSONObject(responseBody)
            val products = json.optJSONArray("products") ?: json.optJSONArray("results")

            if (products == null) {
                Log.e(TAG, "❌ No product array in response")
                Log.e(TAG, "Available keys: ${json.keys().asSequence().toList()}")
                return@withContext emptyList()
            }

            Log.d(TAG, "✅ Found ${products.length()} products")

            for (i in 0 until minOf(products.length(), 20)) {
                try {
                    val product = products.getJSONObject(i)
                    val outfit = parseProduct(product)
                    if (outfit != null) {
                        results.add(outfit)
                        Log.d(TAG, "  ✅ Added: ${outfit.title}")
                        if (results.size >= 5) break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "  ⚠️ Error parsing product $i: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Search exception: ${e.message}", e)
            e.printStackTrace()
        }

        Log.d(TAG, "📦 Returning ${results.size} results for query: '$query'")
        return@withContext results
    }

    /**
     * Parse a single product from ASOS API response
     */
    private fun parseProduct(product: JSONObject): OnlineOutfit? {
        try {
            val name = product.optString("name", "").trim()
            if (name.isEmpty()) {
                Log.d(TAG, "    ⚠️ Product has no name")
                return null
            }

            var imageUrl = extractImageUrl(product)
            if (imageUrl.isEmpty() || imageUrl.contains("placeholder", ignoreCase = true)) {
                Log.d(TAG, "    ⚠️ Invalid image URL for: $name")
                return null
            }

            imageUrl = normalizeImageUrl(imageUrl)
            val price = extractPrice(product)

            var productUrl = product.optString("url", "")
            if (productUrl.startsWith("/")) {
                productUrl = "https://www.asos.com$productUrl"
            }
            if (productUrl.isEmpty()) productUrl = "#"

            return OnlineOutfit(
                title = name,
                imageUrl = imageUrl,
                productUrl = productUrl,
                category = "",
                price = price
            )

        } catch (e: Exception) {
            Log.e(TAG, "    ❌ Error parsing product: ${e.message}")
            return null
        }
    }

    private fun extractImageUrl(product: JSONObject): String {
        try {
            val media = product.optJSONObject("media")
            if (media != null) {
                val images = media.optJSONArray("images")
                if (images != null && images.length() > 0) {
                    val imageUrl = images.getJSONObject(0).optString("url", "")
                    if (imageUrl.isNotEmpty()) return imageUrl
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "    Strategy 1 failed: ${e.message}")
        }

        val directUrl = product.optString("imageUrl", "")
        if (directUrl.isNotEmpty()) return directUrl

        val imageField = product.optString("image", "")
        if (imageField.isNotEmpty()) return imageField

        try {
            val additionalImages = product.optJSONArray("additionalImageUrls")
            if (additionalImages != null && additionalImages.length() > 0) {
                return additionalImages.getString(0)
            }
        } catch (e: Exception) {
            Log.d(TAG, "    Strategy 4 failed: ${e.message}")
        }

        return ""
    }

    private fun normalizeImageUrl(url: String): String {
        var normalized = url.trim()
        if (normalized.startsWith("//")) {
            normalized = "https:$normalized"
        } else if (!normalized.startsWith("http")) {
            normalized = "https://$normalized"
        }

        if (normalized.contains("asos-media.com") && !normalized.contains("?")) {
            normalized = "$normalized?\$n_640w\$&wid=513&fit=constrain"
        }

        return normalized
    }

    private fun extractPrice(product: JSONObject): String {
        try {
            val priceObj = product.optJSONObject("price")
            if (priceObj != null) {
                val current = priceObj.optJSONObject("current")
                if (current != null) {
                    val priceText = current.optString("text", "")
                    if (priceText.isNotEmpty()) {
                        return priceText
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "    Price extraction failed: ${e.message}")
        }
        return ""
    }

    private fun determineGender(bodyData: Map<String, String>): String {
        val genderValue = bodyData["Gender"]?.lowercase() ?: ""
        return when {
            genderValue.contains("female") -> "women"
            genderValue.contains("woman") -> "women"
            genderValue.contains("girl") -> "women"
            else -> "men"
        }
    }

    private fun generateSearchQueries(
        event: String,
        style: String,
        temperature: Double,
        gender: String
    ): List<String> {
        val isHot = temperature > 28
        val isCold = temperature < 20
        val eventLower = event.lowercase()

        return when {
            eventLower.contains("gym") || eventLower.contains("sport") -> listOf(
                "$gender activewear",
                "$gender gym wear",
                "$gender sports clothing"
            )

            eventLower.contains("beach") || eventLower.contains("pool") -> listOf(
                "$gender summer outfit",
                "$gender beach wear",
                "$gender swimwear"
            )

            eventLower.contains("wedding") -> listOf(
                "$gender formal suit",
                "$gender wedding attire",
                "$gender dress shirt"
            )

            eventLower.contains("work") || eventLower.contains("meeting") -> listOf(
                "$gender business casual",
                "$gender office wear",
                "$gender professional"
            )

            eventLower.contains("party") -> listOf(
                "$gender party outfit",
                "$gender evening wear",
                "$gender going out"
            )

            eventLower.contains("date") -> listOf(
                "$gender smart casual",
                "$gender date night",
                "$gender stylish outfit"
            )

            eventLower.contains("concert") -> listOf(
                "$gender streetwear",
                "$gender casual cool",
                "$gender concert outfit"
            )

            eventLower.contains("dinner") -> listOf(
                "$gender smart casual",
                "$gender dinner wear",
                "$gender dressy casual"
            )

            eventLower.contains("casual") || eventLower.contains("shopping") -> when {
                isHot -> listOf(
                    "$gender summer casual",
                    "$gender light clothing",
                    "$gender shorts tshirt"
                )
                isCold -> listOf(
                    "$gender winter casual",
                    "$gender warm clothing",
                    "$gender jacket sweater"
                )
                else -> listOf(
                    "$gender casual wear",
                    "$gender everyday outfit",
                    "$gender comfortable"
                )
            }

            else -> listOf(
                "$gender $style",
                "$gender casual",
                "$gender outfit"
            )
        }
    }

    private fun createErrorOutfit(): OnlineOutfit {
        return OnlineOutfit(
            title = "No items found. Try different preferences!",
            imageUrl = "https://placehold.co/600x800/EEE/999?text=No+Results",
            productUrl = "#",
            category = "error",
            price = ""
        )
    }
}

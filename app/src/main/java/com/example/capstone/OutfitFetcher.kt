package com.example.capstone

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private const val RAPIDAPI_KEY = "311e8a48f4mshee792aa9d4175e4p1c308ejsn5880f766d339"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // 🔹 ASOS RapidAPI fetcher
    suspend fun fetchFromASOS(
        query: String,
        category: String = "",
        eventType: String = ""
    ): List<OnlineOutfit> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineOutfit>()

        try {
            val cleanQuery = query.replace(" ", "%20")
            val url =
                "https://asos2.p.rapidapi.com/products/v2/list?store=US&offset=0&categoryId=4209&limit=48&country=US&sort=freshness&q=$cleanQuery&lang=en-US&sizeSchema=US&currency=USD"

            Log.d(TAG, "🌐 Fetching: $query")

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-host", "asos2.p.rapidapi.com")
                .addHeader("x-rapidapi-key", RAPIDAPI_KEY)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody.isNullOrEmpty()) {
                Log.e(TAG, "❌ API Error: ${response.code}")
                return@withContext emptyList()
            }

            val json = JSONObject(responseBody)
            val products = json.optJSONArray("products") ?: return@withContext emptyList()

            Log.d(TAG, "✅ Found ${products.length()} products")

            for (i in 0 until minOf(products.length(), 48)) {
                try {
                    val product = products.getJSONObject(i)
                    val name = product.optString("name", "")
                    if (name.isEmpty()) continue

                    // ✅ Extract image URL from nested JSON
                    var imageUrl = ""
                    val media = product.optJSONObject("media")
                    val images = media?.optJSONArray("images")
                    if (images != null && images.length() > 0) {
                        imageUrl = images.getJSONObject(0).optString("url", "")
                    }

                    // 🔹 fallback fields if needed
                    if (imageUrl.isEmpty()) {
                        imageUrl = product.optString("imageUrl", "")
                        if (imageUrl.isEmpty()) {
                            imageUrl = product.optString("image", "")
                        }
                    }

                    // 🔹 Fix URL formatting
                    if (imageUrl.startsWith("//")) imageUrl = "https:$imageUrl"
                    if (!imageUrl.startsWith("http")) imageUrl = "https://$imageUrl"

                    // Improve image quality
                    if (imageUrl.contains("asos-media.com") && !imageUrl.contains("?")) {
                        imageUrl = "$imageUrl?\$n_640w\$&wid=513&fit=constrain"
                    }

                    // ✅ Extract price and product link
                    val price = product
                        .optJSONObject("price")
                        ?.optJSONObject("current")
                        ?.optString("text", "")
                        ?: ""

                    var productUrl = product.optString("url", "")
                    if (productUrl.startsWith("/")) {
                        productUrl = "https://www.asos.com$productUrl"
                    }

                    if (name.isNotEmpty() && imageUrl.isNotEmpty() && !imageUrl.contains("placeholder")) {
                        results.add(
                            OnlineOutfit(
                                title = name,
                                imageUrl = imageUrl,
                                productUrl = productUrl,
                                category = category,
                                price = price
                            )
                        )

                        Log.d(TAG, "✅ Added: $name")
                        Log.d(TAG, "   🖼️ Image: $imageUrl")

                        if (results.size >= 5) break
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing product: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Fetch error: ${e.message}")
            e.printStackTrace()
        }

        Log.d(TAG, "📦 Returning ${results.size} items")
        return@withContext results
    }

    // 🔹 Recommendation logic
    suspend fun getOutfitRecommendations(
        eventType: String,
        preferredStyle: String,
        theme: String,
        currentWeather: String,
        temperature: Double,
        bodyData: Map<String, String>
    ): List<OnlineOutfit> = withContext(Dispatchers.IO) {

        val gender = when (bodyData["Gender"]?.lowercase()) {
            "male", "man", "boy" -> "men"
            "female", "woman", "girl" -> "women"
            else -> "men"
        }

        Log.d(TAG, "🧥 Event: $eventType | Style: $preferredStyle | Temp: ${temperature}°C")

        val queries = getSearchQueries(eventType, preferredStyle, temperature, gender)
        Log.d(TAG, "🔍 Queries: $queries")

        val allResults = queries.map { query ->
            async {
                try {
                    fetchFromASOS(query, "", eventType)
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        val finalResults = allResults.distinctBy { it.imageUrl }.take(10)

        if (finalResults.isEmpty()) {
            Log.w(TAG, "⚠️ No outfits found, showing placeholder")
            return@withContext listOf(
                OnlineOutfit(
                    title = "No items found. Try different preferences!",
                    imageUrl = "https://placehold.co/600x800/EEE/999?text=No+Results",
                    productUrl = "#",
                    category = "error"
                )
            )
        }

        finalResults.forEachIndexed { i, outfit ->
            Log.d(TAG, "[$i] ${outfit.title}")
            Log.d(TAG, "   Image: ${outfit.imageUrl}")
        }

        return@withContext finalResults
    }

    private fun getSearchQueries(
        event: String,
        style: String,
        temp: Double,
        gender: String
    ): List<String> {
        val isHot = temp > 28
        val isCold = temp < 20

        return when (event.lowercase()) {
            "gym/sports" -> listOf(
                "$gender athletic wear", "$gender gym clothes", "$gender sportswear"
            )
            "beach/pool" -> listOf(
                "$gender summer clothes", "$gender beachwear", "$gender casual shorts"
            )
            "wedding" -> listOf(
                "$gender formal wear", "$gender dress clothes", "$gender wedding outfit"
            )
            "work meeting" -> listOf(
                "$gender business casual", "$gender office wear", "$gender professional clothes"
            )
            "party" -> listOf(
                "$gender party outfit", "$gender going out clothes", "$gender evening wear"
            )
            "date night" -> listOf(
                "$gender smart casual", "$gender date outfit", "$gender nice clothes"
            )
            "concert" -> listOf(
                "$gender casual wear", "$gender streetwear", "$gender concert outfit"
            )
            "casual outing" -> when {
                isHot -> listOf(
                    "$gender summer casual", "$gender light clothing", "$gender casual wear"
                )
                isCold -> listOf(
                    "$gender winter casual", "$gender warm clothes", "$gender casual wear"
                )
                else -> listOf(
                    "$gender casual clothes", "$gender everyday wear", "$gender comfortable clothing"
                )
            }
            "shopping" -> listOf(
                "$gender casual comfortable", "$gender everyday clothes", "$gender relaxed fit"
            )
            "dinner" -> listOf(
                "$gender smart casual", "$gender dinner outfit", "$gender nice clothes"
            )
            else -> listOf(
                "$gender $style clothes", "$gender casual wear", "$gender everyday outfit"
            )
        }
    }
}

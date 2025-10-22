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
    val price: String = "",
    val source: String = ""
)

object OutfitFetcher {
    private const val TAG = "OutfitFetcher"
    private const val SERPAPI_KEY = "8c74a2679eba166a13a0d95a20ce0411522015d120054c6ecb8d989b76d77706" // Replace with your actual key

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
        Log.d(TAG, "Theme: $theme")
        Log.d(TAG, "Weather: $currentWeather")
        Log.d(TAG, "Temperature: ${temperature}°C")
        Log.d(TAG, "========================================")

        val gender = determineGender(bodyData)
        val bodyType = determineBodyType(bodyData)
        val queries = generateSmartSearchQueries(
            eventType,
            preferredStyle,
            theme,
            temperature,
            gender,
            bodyType
        )

        Log.d(TAG, "🔍 Generated ${queries.size} search queries for $gender ($bodyType):")
        queries.forEachIndexed { index, query ->
            Log.d(TAG, "  [$index] $query")
        }

        // Execute searches in parallel
        val allResults = queries.map { query ->
            async {
                try {
                    searchGoogleShopping(query)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Search failed for '$query': ${e.message}", e)
                    emptyList()
                }
            }
        }.awaitAll().flatten()

        Log.d(TAG, "📦 Total results before filtering: ${allResults.size}")

        // Categorize and filter results
        val categorizedResults = categorizeClothing(allResults)

        Log.d(TAG, "📊 Categorization results:")
        Log.d(TAG, "   👔 Upperwear: ${categorizedResults["upperwear"]?.size ?: 0} items")
        Log.d(TAG, "   👖 Lowerwear: ${categorizedResults["lowerwear"]?.size ?: 0} items")
        Log.d(TAG, "   👟 Shoes: ${categorizedResults["shoes"]?.size ?: 0} items")

        // Get 2 items from each category
        val finalResults = mutableListOf<OnlineOutfit>()
        finalResults.addAll(categorizedResults["upperwear"]?.take(2) ?: emptyList())
        finalResults.addAll(categorizedResults["lowerwear"]?.take(2) ?: emptyList())
        finalResults.addAll(categorizedResults["shoes"]?.take(2) ?: emptyList())

        val filteredResults = finalResults.filter { it.category != "error" }

        Log.d(TAG, "✅ Final results: ${filteredResults.size} items (Target: 6 items - 2 per category)")

        if (filteredResults.isEmpty()) {
            Log.w(TAG, "⚠️ No results found, returning placeholder")
            return@withContext listOf(createErrorOutfit(eventType, preferredStyle))
        }

        filteredResults.forEachIndexed { index, outfit ->
            Log.d(TAG, "[$index] ${outfit.category.uppercase()}: ${outfit.title}")
            Log.d(TAG, "     💰 ${outfit.price}")
            Log.d(TAG, "     🏪 ${outfit.source}")
            Log.d(TAG, "     🖼️ ${outfit.imageUrl.take(60)}...")
        }

        return@withContext filteredResults
    }

    /**
     * Search Google Shopping via SerpAPI
     */
    private suspend fun searchGoogleShopping(query: String): List<OnlineOutfit> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OnlineOutfit>()

        try {
            Log.d(TAG, "🌐 Searching Google Shopping: '$query'")

            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = buildString {
                append("https://serpapi.com/search.json")
                append("?engine=google_shopping")
                append("&q=$encodedQuery")
                append("&api_key=$SERPAPI_KEY")
                append("&num=20") // Get more results
                append("&hl=en")
                append("&gl=us")
            }

            Log.d(TAG, "📡 URL: $url")

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string()

            Log.d(TAG, "📥 Response Code: $responseCode")
            Log.d(TAG, "📥 Response Length: ${responseBody?.length ?: 0} characters")

            if (!response.isSuccessful) {
                Log.e(TAG, "❌ API Error $responseCode: ${response.message}")
                if (responseBody != null) {
                    Log.e(TAG, "Response Body: ${responseBody.take(500)}")
                }
                return@withContext emptyList()
            }

            if (responseBody.isNullOrEmpty()) {
                Log.e(TAG, "❌ Empty response body")
                return@withContext emptyList()
            }

            val json = JSONObject(responseBody)

            // Check for API errors
            val error = json.optString("error", "")
            if (error.isNotEmpty()) {
                Log.e(TAG, "❌ SerpAPI Error: $error")
                return@withContext emptyList()
            }

            val shoppingResults = json.optJSONArray("shopping_results")

            if (shoppingResults == null || shoppingResults.length() == 0) {
                Log.e(TAG, "❌ No shopping results in response")
                return@withContext emptyList()
            }

            Log.d(TAG, "✅ Found ${shoppingResults.length()} products")

            for (i in 0 until minOf(shoppingResults.length(), 20)) {
                try {
                    val product = shoppingResults.getJSONObject(i)
                    val outfit = parseGoogleShoppingProduct(product)
                    if (outfit != null) {
                        results.add(outfit)
                        Log.d(TAG, "  ✅ Added: ${outfit.title}")
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
     * Parse a single product from Google Shopping results
     */
    private fun parseGoogleShoppingProduct(product: JSONObject): OnlineOutfit? {
        try {
            val title = product.optString("title", "").trim()
            if (title.isEmpty()) {
                Log.d(TAG, "    ⚠️ Product has no title")
                return null
            }

            val imageUrl = product.optString("thumbnail", "")
            if (imageUrl.isEmpty()) {
                Log.d(TAG, "    ⚠️ No image URL for: $title")
                return null
            }

            val price = product.optString("price", "")
            val extractedPrice = product.optString("extracted_price", "")
            val displayPrice = if (price.isNotEmpty()) price else if (extractedPrice.isNotEmpty()) "${extractedPrice}" else ""

            // Try multiple possible link fields from SerpAPI
            var link = product.optString("link", "")
            if (link.isEmpty()) link = product.optString("product_link", "")
            if (link.isEmpty()) link = product.optString("url", "")

            // If still no direct link, construct a Google Shopping search URL
            if (link.isEmpty()) {
                val productId = product.optString("product_id", "")
                if (productId.isNotEmpty()) {
                    link = "https://www.google.com/shopping/product/$productId"
                } else {
                    // Last resort: create a Google search URL with the product title
                    val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
                    link = "https://www.google.com/search?tbm=shop&q=$encodedTitle"
                }
            }

            val source = product.optString("source", "Online Store")

            Log.d(TAG, "  ✅ Added: $title")
            Log.d(TAG, "      💰 $displayPrice")
            Log.d(TAG, "      🔗 $link")

            return OnlineOutfit(
                title = title,
                imageUrl = imageUrl,
                productUrl = link,
                category = "",
                price = displayPrice,
                source = source
            )

        } catch (e: Exception) {
            Log.e(TAG, "    ❌ Error parsing product: ${e.message}")
            return null
        }
    }

    private fun determineGender(bodyData: Map<String, String>): String {
        val genderValue = bodyData["Gender"]?.lowercase() ?: ""
        return when {
            genderValue.contains("female") -> "women"
            genderValue.contains("woman") -> "women"
            genderValue.contains("girl") -> "women"
            genderValue.contains("lady") -> "women"
            genderValue.contains("male") -> "men"
            genderValue.contains("man") -> "men"
            genderValue.contains("boy") -> "men"
            else -> "unisex"
        }
    }

    private fun determineBodyType(bodyData: Map<String, String>): String {
        // Extract body measurements to suggest appropriate fits
        val height = bodyData["Height"]?.toDoubleOrNull() ?: 0.0
        val weight = bodyData["Weight"]?.toDoubleOrNull() ?: 0.0

        return when {
            height > 180 -> "tall"
            height < 160 -> "petite"
            weight > 90 -> "plus-size"
            else -> "regular"
        }
    }

    /**
     * Categorize clothing items into upperwear, lowerwear, and shoes
     */
    private fun categorizeClothing(outfits: List<OnlineOutfit>): Map<String, List<OnlineOutfit>> {
        val upperwear = mutableListOf<OnlineOutfit>()
        val lowerwear = mutableListOf<OnlineOutfit>()
        val shoes = mutableListOf<OnlineOutfit>()

        for (outfit in outfits) {
            if (outfit.category == "error") continue
            if (outfit.imageUrl.isEmpty() || outfit.price.isEmpty()) continue

            val titleLower = outfit.title.lowercase()

            when {
                // Shoes
                titleLower.contains("shoe") || titleLower.contains("sneaker") ||
                        titleLower.contains("boot") || titleLower.contains("sandal") ||
                        titleLower.contains("heel") || titleLower.contains("loafer") ||
                        titleLower.contains("slipper") || titleLower.contains("trainer") -> {
                    shoes.add(outfit.copy(category = "shoes"))
                }

                // Lowerwear
                titleLower.contains("pant") || titleLower.contains("jean") ||
                        titleLower.contains("short") || titleLower.contains("skirt") ||
                        titleLower.contains("trouser") || titleLower.contains("legging") ||
                        titleLower.contains("jogger") || titleLower.contains("cargo") -> {
                    lowerwear.add(outfit.copy(category = "lowerwear"))
                }

                // Upperwear (default for shirts, tops, jackets, etc.)
                else -> {
                    upperwear.add(outfit.copy(category = "upperwear"))
                }
            }
        }

        return mapOf(
            "upperwear" to upperwear.distinctBy { it.imageUrl }.take(2),
            "lowerwear" to lowerwear.distinctBy { it.imageUrl }.take(2),
            "shoes" to shoes.distinctBy { it.imageUrl }.take(2)
        )
    }

    /**
     * Generate smart search queries based on event, style, weather, and body type
     */
    private fun generateSmartSearchQueries(
        event: String,
        style: String,
        theme: String,
        temperature: Double,
        gender: String,
        bodyType: String
    ): List<String> {
        val queries = mutableListOf<String>()
        val eventLower = event.lowercase()
        val styleLower = style.lowercase()
        val isHot = temperature > 28
        val isCold = temperature < 20

        // Weather-appropriate clothing terms
        val weatherTerms = when {
            isHot -> listOf("lightweight", "breathable", "summer")
            isCold -> listOf("warm", "layered", "winter")
            else -> listOf("comfortable", "versatile")
        }

        when {
            eventLower.contains("gym") || eventLower.contains("sport") || eventLower.contains("workout") -> {
                queries.add("$gender athletic shirt ${weatherTerms[0]}")
                queries.add("$gender athletic pants shorts")
                queries.add("$gender running shoes sneakers")
            }

            eventLower.contains("beach") || eventLower.contains("pool") || eventLower.contains("swim") -> {
                queries.add("$gender swimwear bikini swim trunks")
                queries.add("$gender beach shorts")
                queries.add("$gender sandals flip flops")
            }

            eventLower.contains("wedding") || eventLower.contains("formal event") -> {
                if (gender == "men") {
                    queries.add("men dress shirt formal")
                    queries.add("men dress pants trousers")
                    queries.add("men formal shoes oxford")
                } else {
                    queries.add("women formal top blouse")
                    queries.add("women formal skirt pants")
                    queries.add("women formal heels")
                }
            }

            eventLower.contains("work") || eventLower.contains("office") || eventLower.contains("meeting") -> {
                queries.add("$gender business shirt blouse")
                queries.add("$gender office pants trousers")
                queries.add("$gender professional shoes")
            }

            eventLower.contains("party") || eventLower.contains("club") -> {
                queries.add("$gender party top shirt")
                queries.add("$gender party pants jeans")
                queries.add("$gender party shoes")
            }

            eventLower.contains("date") || eventLower.contains("romantic") -> {
                queries.add("$gender date night shirt top")
                queries.add("$gender smart casual pants")
                queries.add("$gender casual dressy shoes")
            }

            eventLower.contains("concert") || eventLower.contains("festival") -> {
                queries.add("$gender graphic tshirt shirt")
                queries.add("$gender jeans pants")
                queries.add("$gender sneakers comfortable shoes")
            }

            eventLower.contains("dinner") || eventLower.contains("restaurant") -> {
                queries.add("$gender smart casual shirt")
                queries.add("$gender dress pants jeans")
                queries.add("$gender casual dressy shoes")
            }

            eventLower.contains("casual") || eventLower.contains("everyday") || eventLower.contains("shopping") -> {
                queries.add("$gender casual ${weatherTerms[0]} shirt tshirt")
                queries.add("$gender casual ${weatherTerms[0]} pants shorts")
                queries.add("$gender casual sneakers shoes")
            }

            else -> {
                // Default queries based on style and weather
                queries.add("$gender ${weatherTerms[0]} shirt top")
                queries.add("$gender ${weatherTerms[0]} pants")
                queries.add("$gender casual shoes")
            }
        }

        // Add theme-based query if theme is specified
        if (theme.isNotEmpty() && theme.lowercase() != "none") {
            queries.add("$gender $theme style clothing")
        }

        return queries.take(3) // 3 queries: 1 for upperwear, 1 for lowerwear, 1 for shoes
    }

    private fun extractPriceValue(priceString: String): Double {
        return try {
            priceString.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }

    private fun createErrorOutfit(eventType: String, style: String): OnlineOutfit {
        return OnlineOutfit(
            title = "No items found for $eventType. Try adjusting your preferences!",
            imageUrl = "https://placehold.co/600x800/EEE/999?text=No+Results+Found",
            productUrl = "#",
            category = "error",
            price = "",
            source = "System"
        )
    }
}
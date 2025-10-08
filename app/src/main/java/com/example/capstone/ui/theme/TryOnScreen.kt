
package com.example.capstone.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter


data class ClothingItem(
    val id: String,
    val name: String,
    val category: Category,
    val image: String,
    val color: Color,
    val brand: String? = null,
    val price: Int? = null,
    val rating: Float? = null,
    val weather: List<String>? = null
)

enum class Category { TOPS, BOTTOMS, SHOES, DRESSES, OUTERWEAR }

data class OutfitState(
    val tops: ClothingItem? = null,
    val bottoms: ClothingItem? = null,
    val shoes: ClothingItem? = null,
    val dress: ClothingItem? = null,
    val outerwear: ClothingItem? = null
)


val mockClothingItems = listOf(
    ClothingItem("1", "Classic White Shirt", Category.TOPS,
        "https://images.unsplash.com/photo-1603252110481-7ba873bf42ab",
        Color.White, "Zara", 89, 4.8f, listOf("sunny", "mild")
    ),
    ClothingItem("2", "Elegant Black Dress", Category.DRESSES,
        "https://images.unsplash.com/photo-1720005398225-4ea01c9d2b8f",
        Color.Black, "H&M", 149, 4.9f, listOf("evening", "formal")
    ),
    ClothingItem("3", "Classic Blue Jeans", Category.BOTTOMS,
        "https://images.unsplash.com/photo-1639602182178-2dc689354103",
        Color(0xFF1e3a8a), "Levi's", 120, 4.7f, listOf("casual", "cool")
    ),
    ClothingItem("4", "Navy Blazer", Category.OUTERWEAR,
        "https://images.unsplash.com/photo-1592878849122-facb97520f9e",
        Color(0xFF1e40af), "Hugo Boss", 299, 4.9f, listOf("formal", "professional")
    ),
    ClothingItem("5", "White Sneakers", Category.SHOES,
        "https://images.unsplash.com/photo-1582213153939-613105f23a1b",
        Color.White, "Nike", 180, 4.6f, listOf("casual", "sporty")
    ),
    ClothingItem("6", "Leather Boots", Category.SHOES,
        "https://images.unsplash.com/photo-1495579891230-9592c8ba6708",
        Color(0xFF8B4513), "Dr. Martens", 220, 4.8f, listOf("winter", "formal")
    )
)


fun OutfitState.getItemByCategory(category: Category) = when(category) {
    Category.TOPS -> tops
    Category.BOTTOMS -> bottoms
    Category.SHOES -> shoes
    Category.DRESSES -> dress
    Category.OUTERWEAR -> outerwear
}

fun OutfitState.updateWithSelection(item: ClothingItem): OutfitState {
    return when(item.category) {
        Category.TOPS -> copy(
            tops = if (tops?.id == item.id) null else item,
            dress = null
        )
        Category.BOTTOMS -> copy(
            bottoms = if (bottoms?.id == item.id) null else item,
            dress = null
        )
        Category.DRESSES -> copy(
            dress = if (dress?.id == item.id) null else item,
            tops = null,
            bottoms = null
        )
        Category.SHOES -> copy(
            shoes = if (shoes?.id == item.id) null else item
        )
        Category.OUTERWEAR -> copy(
            outerwear = if (outerwear?.id == item.id) null else item
        )
    }
}


@Composable
fun MobileMannequin(outfit: OutfitState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(52.dp, 64.dp)
                .background(
                    brush = Brush.linearGradient(listOf(Color(0xFFF1F3F4), Color.White)),
                    shape = RoundedCornerShape(50)
                )
        )

        Column(
            modifier = Modifier.padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (outfit.dress != null) {
                Box(
                    modifier = Modifier
                        .size(width = 120.dp, height = 170.dp)
                        .background(outfit.dress.color, shape = RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 70.dp)
                        .background(outfit.tops?.color ?: Color.LightGray, shape = RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 100.dp)
                        .background(outfit.bottoms?.color ?: Color.LightGray, shape = RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Box(
                    modifier = Modifier
                        .size(28.dp, 16.dp)
                        .background(outfit.shoes?.color ?: Color.Gray, shape = RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp, 16.dp)
                        .background(outfit.shoes?.color ?: Color.Gray, shape = RoundedCornerShape(4.dp))
                )
            }
        }
    }
}


@Composable
fun ClothingCard(
    item: ClothingItem,
    isSelected: Boolean,
    onSelect: (ClothingItem) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(4.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF00C8A0) else Color.Gray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect(item) }
    ) {
        Image(
            painter = rememberAsyncImagePainter(item.image),
            contentDescription = item.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(item.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("\$${item.price}", color = Color(0xFF00C8A0), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${item.rating}", fontSize = 10.sp)
        }
    }
}

@Composable
fun TryOnScreen(onNavigate: (String) -> Unit) {
    var outfit by remember { mutableStateOf(OutfitState()) }
    var activeCategory by remember { mutableStateOf(Category.TOPS) }

    val categories = listOf(Category.TOPS, Category.BOTTOMS, Category.DRESSES, Category.OUTERWEAR, Category.SHOES)

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Virtual Try-On") },
            navigationIcon = {
                IconButton(onClick = { onNavigate("assistant") }) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
            },
            actions = {
                IconButton(onClick = { /* toggle like */ }) { Icon(Icons.Default.Favorite, contentDescription = null) }
                IconButton(onClick = { /* share */ }) { Icon(Icons.Default.Share, contentDescription = null) }
            },
            backgroundColor = Color(0xFF00C8A0),
            contentColor = Color.White
        )


        MobileMannequin(outfit)

        LazyRow(modifier = Modifier.padding(8.dp)) {
            items(categories) { category ->
                Button(
                    onClick = { activeCategory = category },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (activeCategory == category) Color(0xFF00C8A0) else Color.LightGray
                    ),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(category.name)
                }
            }
        }


        val filteredItems = mockClothingItems.filter { it.category == activeCategory }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(filteredItems) { item ->
                ClothingCard(
                    item = item,
                    isSelected = outfit.getItemByCategory(item.category)?.id == item.id,
                    onSelect = { selected -> outfit = outfit.updateWithSelection(selected) }
                )
            }
        }

        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { outfit = OutfitState() },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.LightGray),
                modifier = Modifier.weight(1f)
            ) { Text("Reset") }
            Button(
                onClick = { /* Save Look */ },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF00C8A0)),
                modifier = Modifier.weight(1f)
            ) { Text("Save Look", color = Color.White) }
        }
    }
}

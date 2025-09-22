package com.example.capstone.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.Tab
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun TryOnScreen(onNavigate: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf("Tops") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(12.dp)
    ) {
        // Header
        Text(
            text = "Virtual Try-On",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Create your perfect look",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Left side: mannequin placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder mannequin (replace with vector later if available)
                Text("👕", fontSize = 40.sp, color = Color.Gray)
                // Icon(
                //     painter = painterResource(id = R.drawable.ic_hanger), // ❌ commented
                //     contentDescription = "Mannequin",
                //     tint = Color.Gray,
                //     modifier = Modifier.size(100.dp)
                // )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right side: products
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Tabs (Tops / Bottoms)
                TabRow(
                    selectedTabIndex = if (selectedTab == "Tops") 0 else 1,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == "Tops",
                        onClick = { selectedTab = "Tops" },
                        text = { Text("Tops") }
                    )
                    Tab(
                        selected = selectedTab == "Bottoms",
                        onClick = { selectedTab = "Bottoms" },
                        text = { Text("Bottoms") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Example product card
                ProductCard(
                    title = "Classic White Shirt",
                    brand = "Zara",
                    price = "$89",
                    rating = "4.8"
                    // , imageRes = R.drawable.sample_shirt // ❌ commented
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { /* Reset */ },
                shape = CircleShape
            ) {
                Text("Reset")
            }

            Button(
                onClick = { /* Save */ },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)) // green
            ) {
                Text("Save")
            }

            Button(
                onClick = { /* Save Look */ },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
            ) {
                Text("Save Look")
            }

            OutlinedButton(
                onClick = { onNavigate("history") },
                shape = CircleShape
            ) {
                Text("View History")
            }
        }
    }
}

@Composable
fun ProductCard(title: String, brand: String, price: String, rating: String /*, imageRes: Int */) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder instead of image
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("🧥", fontSize = 20.sp)
                // Image(
                //     painter = painterResource(id = imageRes), // ❌ commented
                //     contentDescription = title,
                //     modifier = Modifier
                //         .size(60.dp)
                //         .clip(RoundedCornerShape(8.dp))
                // )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(price, color = Color(0xFF00C853), fontSize = 12.sp)
                Text(brand, fontSize = 12.sp, color = Color.Gray)
                Text("⭐ $rating", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

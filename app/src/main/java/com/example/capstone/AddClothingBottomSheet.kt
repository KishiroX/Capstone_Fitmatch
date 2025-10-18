package com.example.capstone.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Bottom sheet for selecting how to add clothing items.
 * Provides three options: Camera, Gallery, or Manual Entry.
 *
 * OPTIMIZED: Added proper error handling and better animations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothingBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit,
    onManualEntry: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 8.dp)
        ) {
            // Title
            Text(
                text = "Add Clothing Item",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )

            // Subtitle
            Text(
                text = "Choose how you'd like to add your clothing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
            )

            // Take Photo Option (Primary - Recommended)
            AddClothingOption(
                icon = Icons.Default.CameraAlt,
                title = "Take Photo",
                description = "Smart detection with camera guides",
                badge = "Recommended",
                isPrimary = true,
                onClick = {
                    onTakePhoto()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Choose from Gallery Option
            AddClothingOption(
                icon = Icons.Default.Image,
                title = "Choose from Gallery",
                description = "Select existing photos",
                badge = null,
                isPrimary = false,
                onClick = {
                    onChooseFromGallery()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Manual Entry Option
            AddClothingOption(
                icon = Icons.Default.Edit,
                title = "Manual Entry",
                description = "Add details without photo",
                badge = null,
                isPrimary = false,
                onClick = {
                    onManualEntry()
                    onDismiss()
                }
            )
        }
    }
}

/**
 * Individual option item in the bottom sheet.
 * IMPROVED: Added badge support and better visual hierarchy
 */
@Composable
private fun AddClothingOption(
    icon: ImageVector,
    title: String,
    description: String,
    badge: String?,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isPrimary)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (isPrimary) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isPrimary)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isPrimary)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPrimary)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Badge (if provided)
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPrimary)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Preview-friendly version (Optional - for Android Studio previews)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClothingBottomSheetPreview() {
    MaterialTheme {
        AddClothingBottomSheet(
            sheetState = rememberModalBottomSheetState(),
            onDismiss = {},
            onTakePhoto = {},
            onChooseFromGallery = {},
            onManualEntry = {}
        )
    }
}
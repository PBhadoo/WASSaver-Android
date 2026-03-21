package com.wassaver.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.wassaver.app.data.model.MediaFilter
import com.wassaver.app.data.model.StatusFile
import com.wassaver.app.ui.theme.WhatsAppGreen
import com.wassaver.app.viewmodel.StatusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: StatusViewModel,
    onStatusClick: (StatusFile, Int, List<StatusFile>) -> Unit
) {
    val savedStatuses by viewModel.savedFilteredStatuses.collectAsState()
    val savedFilter by viewModel.savedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadSavedStatuses()
    }

    // Reset selection when exiting selection mode
    LaunchedEffect(selectionMode) {
        if (!selectionMode) {
            selectedItems = emptySet()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (selectionMode) MaterialTheme.colorScheme.secondaryContainer 
                    else MaterialTheme.colorScheme.primary,
            shadowElevation = 4.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectionMode) {
                        IconButton(onClick = { selectionMode = false }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Exit selection",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedItems.size} selected",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Saved Statuses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (selectionMode) {
                        // Select All button
                        TextButton(
                            onClick = {
                                selectedItems = if (selectedItems.size == savedStatuses.size) {
                                    emptySet()
                                } else {
                                    savedStatuses.map { it.uri.toString() }.toSet()
                                }
                            }
                        ) {
                            Text(
                                if (selectedItems.size == savedStatuses.size) "Deselect All" else "Select All",
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        // Delete button
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            enabled = selectedItems.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = if (selectedItems.isNotEmpty()) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        if (savedStatuses.isNotEmpty()) {
                            IconButton(onClick = { selectionMode = true }) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Select mode",
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.loadSavedStatuses() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Filter chips - hide in selection mode
                AnimatedVisibility(visible = !selectionMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MediaFilter.entries.forEach { filter ->
                            val isSelected = savedFilter == filter
                            AssistChip(
                                onClick = { viewModel.selectSavedFilter(filter) },
                                label = {
                                    Text(
                                        text = filter.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (filter) {
                                            MediaFilter.ALL -> Icons.Default.GridView
                                            MediaFilter.PHOTOS -> Icons.Default.Image
                                            MediaFilter.VIDEOS -> Icons.Default.VideoLibrary
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                    leadingIconContentColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        }

        // Content
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading saved statuses...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (savedStatuses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOff,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Saved Statuses",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Statuses you save will appear here. Go to the home tab to view and save statuses.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            SavedStatusGrid(
                statuses = savedStatuses,
                selectionMode = selectionMode,
                selectedItems = selectedItems,
                onStatusClick = { status, index ->
                    if (selectionMode) {
                        val uri = status.uri.toString()
                        selectedItems = if (uri in selectedItems) {
                            selectedItems - uri
                        } else {
                            selectedItems + uri
                        }
                    } else {
                        onStatusClick(status, index, savedStatuses)
                    }
                },
                onDeleteClick = { status ->
                    viewModel.deleteSavedStatus(status)
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Delete ${selectedItems.size} status${if (selectedItems.size > 1) "es" else ""}?")
            },
            text = {
                Text("This action cannot be undone. The files will be permanently deleted from your device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = savedStatuses.filter { it.uri.toString() in selectedItems }
                        viewModel.deleteSavedStatuses(toDelete)
                        showDeleteDialog = false
                        selectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SavedStatusGrid(
    statuses: List<StatusFile>,
    selectionMode: Boolean,
    selectedItems: Set<String>,
    onStatusClick: (StatusFile, Int) -> Unit,
    onDeleteClick: (StatusFile) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(statuses, key = { it.uri.toString() }) { status ->
            SavedStatusGridItem(
                status = status,
                selectionMode = selectionMode,
                isSelected = status.uri.toString() in selectedItems,
                onClick = {
                    val index = statuses.indexOf(status)
                    onStatusClick(status, index)
                },
                onDeleteClick = { onDeleteClick(status) }
            )
        }
    }
}

@Composable
private fun SavedStatusGridItem(
    status: StatusFile,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Thumbnail
            val imageRequest = if (status.isVideo) {
                ImageRequest.Builder(context)
                    .data(status.uri)
                    .decoderFactory(VideoFrameDecoder.Factory())
                    .crossfade(true)
                    .build()
            } else {
                ImageRequest.Builder(context)
                    .data(status.uri)
                    .crossfade(true)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = status.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Selection overlay
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isSelected) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else 
                                Color.Transparent
                        )
                )
            }

            // Video play icon overlay
            if (status.isVideo && !selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Selection checkbox
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                Color.White.copy(alpha = 0.8f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Bottom gradient overlay with delete button
            if (!selectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (status.isVideo) Color(0xFFFF6B6B) else WhatsAppGreen,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = if (status.isVideo) "VIDEO" else "PHOTO",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Delete button
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Individual delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Delete this status?")
            },
            text = {
                Text("This action cannot be undone. The file will be permanently deleted from your device.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

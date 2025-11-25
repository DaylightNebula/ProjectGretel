package daylightnebula.projectgretel.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import daylightnebula.projectgretel.DatabaseHelper
import daylightnebula.projectgretel.data.Trail
import daylightnebula.projectgretel.ui.theme.Purple40
import daylightnebula.projectgretel.ui.theme.PurpleGrey80
import java.util.UUID

@Composable
fun HomeComponent(
    context: Context,
    onStartTrail: () -> Unit,
    onFollowTrail: (UUID) -> Unit
) {
    var trails by remember { mutableStateOf(getAllTrailsSorted(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hello, welcome to Trails!")

        Button(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonColors(
                containerColor = Purple40,
                contentColor = Color.White,
                disabledContentColor = Color.Black,
                disabledContainerColor = Color.Red
            ),
            onClick = onStartTrail
        ) {
            Text("Click To Start Your Trail!")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .border(
                    border = BorderStroke(2.dp, Color.Gray),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(trails, key = Trail::id) { trail ->
                    TrailItem(
                        context = context,
                        trail = trail,
                        refresh = { trails = getAllTrailsSorted(context) },
                        onFollowTrail = onFollowTrail
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonColors(
                containerColor = PurpleGrey80,
                contentColor = Color.Black,
                disabledContentColor = Color.Black,
                disabledContainerColor = Color.Red
            ),
            onClick = {
                trails = getAllTrailsSorted(context)
            }
        ) {
            Text("Refresh")
        }
    }
}

@Composable
private fun TrailItem(
    context: Context,
    trail: Trail,
    refresh: () -> Unit,
    onFollowTrail: (UUID) -> Unit
) {
    var itemName by remember { mutableStateOf(trail.name) }
    var isFavorite by remember { mutableStateOf(trail.isFavorite) }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleted by remember { mutableStateOf(false) }

    RenameDialog(
        showDialog = showRenameDialog,
        currentName = itemName,
        onDismiss = { showRenameDialog = false },
        onConfirm = { text ->
            DatabaseHelper
                .getInstance(context)
                .renameTrail(trail.id, text)
            itemName = text
            showRenameDialog = false
            refresh()
        }
    )

    DeleteConfirmDialog(
        showDialog = showDeleteDialog,
        itemName = itemName,
        onDismiss = { showDeleteDialog = false },
        onConfirm = {
            DatabaseHelper
                .getInstance(context)
                .deleteTrail(trail.id)
            isDeleted = true
            showDeleteDialog = false
            refresh()
        }
    )

    if (!isDeleted) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onFollowTrail(trail.id)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.Green
                        )
                    }

                    IconButton(
                        onClick = {
                            isFavorite = !isFavorite
                            DatabaseHelper
                                .getInstance(context)
                                .setFavorite(trailId = trail.id, favorite = isFavorite)
                            refresh()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFFD700) else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = { showRenameDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Rename",
                            tint = Purple40
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(
    showDialog: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    if (showDialog) {
        // Create a separate state for the text field inside the dialog
        var newNameText by remember { mutableStateOf(currentName) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Rename Item")
            },
            text = {
                OutlinedTextField(
                    value = newNameText,
                    onValueChange = { newNameText = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(newNameText) },
                    enabled = newNameText.isNotBlank()
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeleteConfirmDialog(
    showDialog: Boolean,
    itemName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = "Delete Trail: $itemName")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun getAllTrailsSorted(context: Context): List<Trail> =
    DatabaseHelper
        .getInstance(context)
        .getAllTrails()
        .sortedWith(
            comparator =
                compareBy<Trail> { !it.isFavorite }
                    .thenBy { it.name.startsWith("TRAIL") }
                    .thenBy { it.name }
        )

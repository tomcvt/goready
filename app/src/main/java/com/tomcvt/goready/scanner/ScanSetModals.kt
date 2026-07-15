package com.tomcvt.goready.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tomcvt.goready.LocalOverlayHost
import com.tomcvt.goready.LocalScanSetRepositoryProvider
import com.tomcvt.goready.data.ScanSetEntity
import com.tomcvt.goready.ui.AlertDialogModal
import com.tomcvt.goready.ui.DialogModal
import kotlinx.coroutines.launch

/**
 * Reusable card for a single [ScanSetEntity] row in the load list.
 * Mirrors the style of [ScanCodeItemCard]: name + count on the left,
 * a delete [IconButton] on the right that opens a confirmation dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanSetLoadCard(
    set: ScanSetEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overlayHost = LocalOverlayHost.current

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = set.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${set.size} code${if (set.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    overlayHost.show { dismiss ->
                        AlertDialogModal(
                            onDismissRequest = dismiss,
                            title = { Text("Delete \"${set.name}\"?") },
                            text = { Text("This set will be permanently deleted.") },
                            confirmButton = {
                                TextButton(onClick = { onDelete(); dismiss() }) { Text("Delete") }
                            },
                            dismissButton = {
                                TextButton(onClick = dismiss) { Text("Cancel") }
                            }
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete set",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Inline modal that shows a lazy list of saved [ScanSetEntity] records.
 * Tapping a card calls [onSelected] with the chosen set.
 * Shown via [com.tomcvt.goready.ui.overlay.OverlayHost].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadSetModal(
    onSelected: (ScanSetEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val repository = LocalScanSetRepositoryProvider.current
    val sets by repository.getScanSets().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    DialogModal(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Load set",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                if (sets.isEmpty()) {
                    Text(
                        text = "No saved sets yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sets, key = { it.id }) { set ->
                            ScanSetLoadCard(
                                set = set,
                                onClick = { onSelected(set) },
                                onDelete = { scope.launch { repository.deleteScanSet(set) } }
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Inline modal that lets the user enter a name and save the current
 * [currentCodes] list as a new [ScanSetEntity].
 * [onDone] receives the inserted entity (with its generated id).
 */
@Composable
fun SaveSetModal(
    currentCodes: List<ScanCode>,
    initialName: String = "",
    onDone: (ScanSetEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val repository = LocalScanSetRepositoryProvider.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialName) }

    DialogModal(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Save as new set",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Set name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            scope.launch {
                                val encoded = repository.encodeScanCodes(currentCodes)
                                val entity = ScanSetEntity(
                                    name = trimmedName,
                                    encodedCodes = encoded,
                                    size = currentCodes.size
                                )
                                val id = repository.insertScanSet(entity)
                                onDone(entity.copy(id = id))
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

package com.example.mobile_app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobile_app.data.model.chat.ChatUserDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChatDialog(
    users: List<ChatUserDto>,
    currentUserId: Long?,
    onDismiss: () -> Unit,
    onCreateDirect: (Long) -> Unit,
    onCreateGroup: (String, Set<Long>) -> Unit,
) {
    var isGroup by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var selectedDirectUserId by remember { mutableStateOf<Long?>(null) }
    var selectedGroupUserIds by remember { mutableStateOf(setOf<Long>()) }

    val selectableUsers = users.filter { it.id != currentUserId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear chat") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row {
                        RadioButton(selected = !isGroup, onClick = { isGroup = false })
                        Text("Individual")
                    }
                    Row {
                        RadioButton(selected = isGroup, onClick = { isGroup = true })
                        Text("Grupal")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isGroup) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre del grupo") },
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Usuarios del grupo")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        LazyColumn {
                            items(selectableUsers) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(user.name)
                                        Text(user.email)
                                    }
                                    Checkbox(
                                        checked = selectedGroupUserIds.contains(user.id),
                                        onCheckedChange = { checked ->
                                            selectedGroupUserIds = if (checked) {
                                                selectedGroupUserIds + user.id
                                            } else {
                                                selectedGroupUserIds - user.id
                                            }
                                        },
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                } else {
                    Text("Selecciona un usuario")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        LazyColumn {
                            items(selectableUsers) { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(user.name)
                                        Text(user.email)
                                    }
                                    RadioButton(
                                        selected = selectedDirectUserId == user.id,
                                        onClick = { selectedDirectUserId = user.id },
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isGroup) {
                        onCreateGroup(groupName.trim(), selectedGroupUserIds)
                    } else {
                        selectedDirectUserId?.let(onCreateDirect)
                    }
                },
                enabled = if (isGroup) {
                    groupName.isNotBlank() && selectedGroupUserIds.isNotEmpty()
                } else {
                    selectedDirectUserId != null
                },
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}


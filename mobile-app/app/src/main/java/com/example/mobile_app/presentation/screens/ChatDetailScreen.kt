package com.example.mobile_app.presentation.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mobile_app.data.model.chat.ChatState
import com.example.mobile_app.data.model.chat.LocalChatMessageRecord
import com.example.mobile_app.presentation.chat.ChatCoordinator
import kotlinx.coroutines.launch

@Composable
fun ChatDetailScreen(
    chatKey: String,
    navController: NavController,
    chatCoordinator: ChatCoordinator,
) {
    val state by chatCoordinator.state.collectAsState(initial = ChatState())
    val chat = state.chats.firstOrNull { it.chatKey == chatKey }
    val messages = state.messages.filter { it.chatKey == chatKey }
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            navController.navigate("chat-members/${Uri.encode(chatKey)}")
                        },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = chat?.title ?: "Chat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (chat?.type == "GROUP") {
                                "Toca para ver los miembros del grupo"
                            } else {
                                "Toca para ver el contacto"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                chatCoordinator.deleteChatLocally(chatKey)
                                navController.popBackStack()
                            }
                        },
                    ) {
                        Text("Eliminar chat")
                    }
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Volver")
                    }
                }
                HorizontalDivider()
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Escribe un mensaje") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val text = messageText.trim()
                        if (text.isBlank()) return@Button
                        scope.launch {
                            if (chatCoordinator.sendMessage(chatKey, text)) {
                                messageText = ""
                            }
                        }
                    },
                    enabled = messageText.isNotBlank(),
                ) {
                    Text("Enviar")
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    showSender = chat?.type == "GROUP",
                )
            }
        }
    }
}

@Composable
fun ChatMembersScreen(
    chatKey: String,
    navController: NavController,
    chatCoordinator: ChatCoordinator,
) {
    val state by chatCoordinator.state.collectAsState(initial = ChatState())
    val chat = state.chats.firstOrNull { it.chatKey == chatKey }
    val members = remember(chat) {
        if (chat == null) emptyList() else chat.memberIds.zip(chat.memberNames)
    }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = chat?.title ?: "Miembros",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                members.forEach { (_, name) ->
                    Text(text = name)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        chatCoordinator.deleteChatLocally(chatKey)
                        navController.popBackStack()
                    }
                },
            ) {
                Text("Eliminar chat")
            }
            Button(onClick = { navController.popBackStack() }) {
                Text("Volver")
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: LocalChatMessageRecord,
    showSender: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isOutgoing) Alignment.End else Alignment.Start,
    ) {
        Card {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (showSender && !message.isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.createdAt.substringAfter('T').take(5),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
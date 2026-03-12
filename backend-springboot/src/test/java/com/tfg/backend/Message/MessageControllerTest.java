package com.tfg.backend.Message;

import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.Security.CustomUserDetailsService;
import com.tfg.backend.Security.JwtAuthenticationFilter;
import com.tfg.backend.User.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void list_returnsMessages_whenMessagesExist() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        Message message = buildMessage(10L, sender, receiver, "Hola");

        when(messageService.list()).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].content").value("Hola"));
    }

    @Test
    void get_returnsNotFound_whenMessageDoesNotExist() throws Exception {
        when(messageService.getById(99L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/messages/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated_whenSenderAndReceiverExist() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        Message savedMessage = buildMessage(11L, sender, receiver, "Mensaje nuevo");

        when(messageService.create(any())).thenReturn(savedMessage);

        String body = """
                {
                  "senderId": 1,
                  "oneToOneChatId": 2,
                  "content": "Mensaje nuevo"
                }
                """;

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.content").value("Mensaje nuevo"));
    }

    @Test
    void create_returnsBadRequest_whenSenderDoesNotExist() throws Exception {
        when(messageService.create(any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        String body = """
                {
                  "senderId": 1,
                  "oneToOneChatId": 2,
                  "content": "Mensaje nuevo"
                }
                """;

        mockMvc.perform(post("/api/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsOk_whenMessageExists() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        Message savedMessage = buildMessage(12L, sender, receiver, "Actualizado");

        when(messageService.update(any(), any())).thenReturn(savedMessage);

        String body = """
                {
                  "content": "Actualizado"
                }
                """;

        mockMvc.perform(put("/api/messages/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Actualizado"));
    }

    @Test
    void delete_returnsNoContent_whenMessageExists() throws Exception {
        org.mockito.Mockito.doNothing().when(messageService).delete(13L);

        mockMvc.perform(delete("/api/messages/13"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getConversation_returnsMessages_whenChatExists() throws Exception {
        User user1 = buildUser(1L, "user1@example.com");
        User user2 = buildUser(2L, "user2@example.com");
        OneToOneChat chat = new OneToOneChat(user1, user2);
        setChatId(chat, 20L);
        Message message = buildMessage(14L, user1, user2, "Conversacion");

        when(messageService.getConversation(1L, 2L)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages/conversation/1/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(14L))
                .andExpect(jsonPath("$[0].content").value("Conversacion"));
    }

    @Test
    void getUnreadMessages_returnsUnreadMessages_forUserChats() throws Exception {
        User sender = buildUser(2L, "sender@example.com");
        User receiver = buildUser(1L, "receiver@example.com");
        Message unreadMessage = buildMessage(15L, sender, receiver, "No leido");

        when(messageService.getUnreadMessages(1L)).thenReturn(List.of(unreadMessage));

        mockMvc.perform(get("/api/messages/unread/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15L))
                .andExpect(jsonPath("$[0].content").value("No leido"));
    }

    @Test
    void markAsRead_returnsOk_whenMessageExists() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        Message savedMessage = buildMessage(16L, sender, receiver, "Pendiente");
        savedMessage.setRead(true);

        when(messageService.markAsRead(16L)).thenReturn(savedMessage);

        mockMvc.perform(put("/api/messages/16/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User");
        user.setEmail(email);
        user.setPassword("StrongP@ss1");
        user.setPublicKey("public-key-" + id);
        return user;
    }

    private Message buildMessage(Long id, User sender, User receiver, String content) {
        OneToOneChat chat = new OneToOneChat(sender, receiver);
        Message message = new Message(sender, content, chat);
        message.setId(id);
        return message;
    }

    private void setChatId(OneToOneChat chat, Long id) throws Exception {
        java.lang.reflect.Field field = OneToOneChat.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(chat, id);
    }
}
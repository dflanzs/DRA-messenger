package com.tfg.backend.Message;

import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.User.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageTest {

    @Test
    void constructor_setsDefaultValues_forOneToOneChatMessage() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);

        Message message = new Message(sender, "Hola", chat);

        assertEquals(sender, message.getSender());
        assertEquals("Hola", message.getContent());
        assertEquals(chat, message.getOneToOneChat());
        assertNotNull(message.getCreatedAt());
        assertFalse(message.isRead());
    }

    @Test
    void getReceiverId_returnsOtherUserId_whenSenderIsFirstUserInChat() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);
        Message message = new Message(sender, "Hola", chat);

        Long receiverId = message.getReceiverId();

        assertEquals(2L, receiverId);
    }

    @Test
    void getReceiverId_returnsNull_whenMessageHasNoOneToOneChat() {
        User sender = buildUser(1L, "sender@example.com");
        Message message = new Message();
        message.setSender(sender);

        Long receiverId = message.getReceiverId();

        assertNull(receiverId);
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
}
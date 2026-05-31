package com.tfg.backend.Message;

import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.SignalEnvelope.SignalEnvelope;
import com.tfg.backend.GroupChat.GroupChat;
import com.tfg.backend.User.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


class SignalEnvelopeTest {

    @Test
    void constructor_setsDefaultValues_forOneToOneChatEnvelope() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);

        byte[] cypherText = "Hola".getBytes();
        Short cypherTextType = 1;
        SignalEnvelope envelope = new SignalEnvelope(sender, receiver, chat, cypherText, cypherTextType);

        assertEquals(sender, envelope.getSender());
        assertEquals(receiver, envelope.getReceiver());
        assertEquals(chat, envelope.getOneToOneChat());
        assertNotNull(envelope.getCreatedAt());
        assertEquals("DIRECT", envelope.getConversationType());
    }

    @Test
    void groupEnvelope_setsGroupChat_andType() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        GroupChat groupChat = new GroupChat();
        byte[] cypherText = "Hola grupo".getBytes();
        Short cypherTextType = 2;
        SignalEnvelope envelope = new SignalEnvelope(sender, receiver, groupChat, cypherText, cypherTextType);

        assertEquals(sender, envelope.getSender());
        assertEquals(receiver, envelope.getReceiver());
        assertEquals(groupChat, envelope.getGroupChat());
        assertNotNull(envelope.getCreatedAt());
        assertEquals("GROUP", envelope.getConversationType());
    }

    @Test
    void envelope_withoutChat_isInconsistent() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        SignalEnvelope envelope = new SignalEnvelope();
        envelope.SetSender(sender);
        envelope.SetReceiver(receiver);
        // No chat set
        assertFalse(envelope.IsConversationConsistent());
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
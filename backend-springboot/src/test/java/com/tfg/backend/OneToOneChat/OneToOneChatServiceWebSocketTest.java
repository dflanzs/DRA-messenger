package com.tfg.backend.OneToOneChat;

import com.tfg.backend.SignalEnvelope.SignalEnvelope;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OneToOneChatServiceWebSocketTest {

    @Mock
    private SignalEnvelopeRepository signalEnvelopeRepository;

    @Mock
    private OneToOneChatRepository oneToOneChatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TrustCirclesService trustCirclesService;


    @Test
    void sendPrivateMessage_routesToReceiverEmail_notSenderEmail() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);

        // Prepare DTO
        SignalDirectMessageRequestDto dto = new SignalDirectMessageRequestDto();
        setField(dto, "recipientUserId", 2L);
        setField(dto, "conversationId", 10L);
        setField(dto, "cypherTextType", (short) 1);
        setField(dto, "cypherTextB64", java.util.Base64.getEncoder().encodeToString("hola".getBytes()));

        byte[] cypherText = "hola".getBytes();
        Short cypherTextType = 1;
        SignalEnvelope persisted = new SignalEnvelope(sender, receiver, chat, cypherText, cypherTextType);
        setIdAndCreatedAt(persisted, 99L, LocalDateTime.of(2026, 4, 9, 12, 0, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(oneToOneChatRepository.findById(10L)).thenReturn(Optional.of(chat));
        when(signalEnvelopeRepository.save(any(SignalEnvelope.class))).thenReturn(persisted);

        // Verify repository and messaging interactions
        verify(trustCirclesService).validateUsersCanCommunicate(1L, 2L);
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate).convertAndSendToUser(
            userCaptor.capture(),
            destinationCaptor.capture(),
            any()
        );
        assertEquals("receiver@example.com", userCaptor.getValue());
        assertEquals("/queue/signal-messages", destinationCaptor.getValue());
    }

    // Helper to set private fields in DTOs
    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void setIdAndCreatedAt(SignalEnvelope envelope, Long id, LocalDateTime createdAt) {
        try {
            java.lang.reflect.Field idField = SignalEnvelope.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(envelope, id);
            java.lang.reflect.Field createdAtField = SignalEnvelope.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(envelope, createdAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail(email);
        user.setPassword("StrongP@ss1");
        user.setPublicKey("pk-" + id);
        return user;
    }
}

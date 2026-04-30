package com.tfg.backend.OneToOneChat;

import com.tfg.backend.SignalEnvelope.Message;
import com.tfg.backend.SignalEnvelope.MessageRepository;
import com.tfg.backend.SignalEnvelope.dto.SendMessageDTO;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OneToOneChatServiceWebSocketTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private OneToOneChatRepository oneToOneChatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TrustCirclesService trustCirclesService;

    private OneToOneChatService oneToOneChatService;

    @BeforeEach
    void setUp() {
        oneToOneChatService = new OneToOneChatService(
            messageRepository,
            oneToOneChatRepository,
            userRepository,
            messagingTemplate,
            trustCirclesService
        );
    }

    @Test
    void sendPrivateMessage_routesToReceiverEmail_notSenderEmail() {
        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);

        SendMessageDTO dto = new SendMessageDTO();
        dto.setOneToOneChatId(10L);
        dto.setContent("hola");

        Message persisted = new Message(sender, "hola", chat);
        persisted.setId(99L);
        persisted.setCreatedAt(LocalDateTime.of(2026, 4, 9, 12, 0, 0));

        Principal principal = () -> "sender@example.com";

        when(userRepository.findByEmailAndDeletedAtIsNull("sender@example.com"))
            .thenReturn(Optional.of(sender));
        when(oneToOneChatRepository.findById(10L)).thenReturn(Optional.of(chat));
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenReturn(persisted);

        oneToOneChatService.sendPrivateMessage(dto, principal);

        verify(trustCirclesService).validateUsersCanCommunicate(1L, 2L);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SendMessageDTO> payloadCaptor = ArgumentCaptor.forClass(SendMessageDTO.class);

        verify(messagingTemplate).convertAndSendToUser(
            userCaptor.capture(),
            destinationCaptor.capture(),
            payloadCaptor.capture()
        );

        assertEquals("receiver@example.com", userCaptor.getValue());
        assertEquals("/queue/messages", destinationCaptor.getValue());

        SendMessageDTO sentPayload = payloadCaptor.getValue();
        assertNotNull(sentPayload);
        assertEquals(99L, sentPayload.getId());
        assertEquals(1L, sentPayload.getSenderId());
        assertEquals("hola", sentPayload.getContent());
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

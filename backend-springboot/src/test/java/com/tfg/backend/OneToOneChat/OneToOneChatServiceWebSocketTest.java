package com.tfg.backend.OneToOneChat;

import com.tfg.backend.GroupChat.GroupChatRepository;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeService;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;
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
    private UserRepository userRepository;

    @Mock
    private OneToOneChatRepository oneToOneChatRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserService userService;

    @Mock
    private SignalEnvelopeRepository signalEnvelopeRepository;

    @Mock
    private GroupChatRepository groupChatRepository;

    @Test
    void sendPrivateMessage_routesToReceiverEmail_notSenderEmail() {
        SignalEnvelopeService service = new SignalEnvelopeService(
            userRepository,
            oneToOneChatRepository,
            messagingTemplate,
            userService,
            signalEnvelopeRepository,
            groupChatRepository
        );

        User sender = buildUser(1L, "sender@example.com");
        User receiver = buildUser(2L, "receiver@example.com");
        OneToOneChat chat = new OneToOneChat(sender, receiver);
        setField(chat, "id", 10L);

        SignalDirectMessageRequestDto dto = new SignalDirectMessageRequestDto();
        setField(dto, "recipientUserId", 2L);
        setField(dto, "conversationId", 10L);
        setField(dto, "cypherTextType", (short) 1);
        setField(dto, "cypherTextB64", java.util.Base64.getEncoder().encodeToString("hola".getBytes()));

        when(userService.getById(2L)).thenReturn(receiver);
        when(userService.getById(1L)).thenReturn(sender);
        when(oneToOneChatRepository.findChatBetweenUsers(1L, 2L)).thenReturn(chat);

        service.sendPrivateMessage(1L, dto);

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

    private void setField(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
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

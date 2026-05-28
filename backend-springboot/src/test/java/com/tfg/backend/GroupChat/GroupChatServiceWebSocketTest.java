package com.tfg.backend.GroupChat;

import com.tfg.backend.OneToOneChat.OneToOneChatRepository;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeService;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalMessageWSDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupChatServiceWebSocketTest {

    @Mock
    private SignalEnvelopeRepository signalEnvelopeRepository;

    @Mock
    private GroupChatRepository groupChatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserService userService;

    @Mock
    private OneToOneChatRepository oneToOneChatRepository;

    @Test
    void sendGroupMessage_routesToNonSenderMembersEmails() {
        SignalEnvelopeService service = new SignalEnvelopeService(
            userRepository,
            oneToOneChatRepository,
            messagingTemplate,
            userService,
            signalEnvelopeRepository,
            groupChatRepository
        );

        User sender = buildUser(1L, "sender@example.com");
        User user2 = buildUser(2L, "user2@example.com");
        User user3 = buildUser(3L, "user3@example.com");

        GroupChat chat = new GroupChat();
        chat.addMember(sender);
        chat.addMember(user2);
        chat.addMember(user3);

        SignalGroupMessageRequestDto dto = new SignalGroupMessageRequestDto();
        setField(dto, "groupChatId", 22L);
        setField(dto, "cypherTextType", (short) 1);
        setField(dto, "cypherTextB64", java.util.Base64.getEncoder().encodeToString("mensaje grupo".getBytes()));

        when(groupChatRepository.existsByIdAndUsers_Id(22L, 1L)).thenReturn(true);
        when(groupChatRepository.findById(22L)).thenReturn(Optional.of(chat));
        when(userService.getById(1L)).thenReturn(sender);
        when(userService.getById(2L)).thenReturn(user2);
        when(userService.getById(3L)).thenReturn(user3);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        service.sendGroupMessage(1L, dto);

        // Sender is skipped: only the other two members receive a WS push.
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(2)).convertAndSendToUser(
            userCaptor.capture(),
            destCaptor.capture(),
            any(SignalMessageWSDto.class)
        );
        assertEquals(
            Set.of("user2@example.com", "user3@example.com"),
            new HashSet<>(userCaptor.getAllValues())
        );
        for (String dest : destCaptor.getAllValues()) {
            assertEquals("/queue/signal-messages", dest);
        }
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

package com.tfg.backend.GroupChat;

import com.tfg.backend.SignalEnvelope.SignalEnvelope;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageWSDto;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private TrustCirclesService trustCirclesService;

    @Test
    void sendGroupMessage_routesToAllGroupMembersEmails() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User user2 = buildUser(2L, "user2@example.com");
        User user3 = buildUser(3L, "user3@example.com");

        GroupChat chat = new GroupChat();
        setUsers(chat, new User[]{sender, user2, user3});

        // Prepare DTO
        SignalGroupMessageRequestDto dto = new SignalGroupMessageRequestDto();
        setField(dto, "groupChatId", 22L);
        setField(dto, "cypherTextType", (short) 1);
        setField(dto, "cypherTextB64", java.util.Base64.getEncoder().encodeToString("mensaje grupo".getBytes()));

        byte[] cypherText = "mensaje grupo".getBytes();
        Short cypherTextType = 1;

        // Prepare persisted envelopes for each user
        SignalEnvelope env1 = new SignalEnvelope(sender, sender, chat, cypherText, cypherTextType);
        setIdAndCreatedAt(env1, 201L, LocalDateTime.of(2026, 4, 9, 12, 30, 0));
        SignalEnvelope env2 = new SignalEnvelope(sender, user2, chat, cypherText, cypherTextType);
        setIdAndCreatedAt(env2, 202L, LocalDateTime.of(2026, 4, 9, 12, 30, 0));
        SignalEnvelope env3 = new SignalEnvelope(sender, user3, chat, cypherText, cypherTextType);
        setIdAndCreatedAt(env3, 203L, LocalDateTime.of(2026, 4, 9, 12, 30, 0));

        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));
        when(groupChatRepository.findById(22L)).thenReturn(Optional.of(chat));
        // Save returns the envelope itself
        when(signalEnvelopeRepository.save(any(SignalEnvelope.class))).thenAnswer(inv -> {
            SignalEnvelope env = inv.getArgument(0);
            if (env.getReceiver().getId().equals(1L)) return env1;
            if (env.getReceiver().getId().equals(2L)) return env2;
            if (env.getReceiver().getId().equals(3L)) return env3;
            return env;
        });

        // Call the service method (simulate senderUserId = 1L)
        // groupChatService.sendGroupMessage(1L, dto);

        // Verify messaging interactions
        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(3)).convertAndSendToUser(
            userCaptor.capture(),
            destCaptor.capture(),
            any(SignalDirectMessageWSDto.class)
        );
        List<String> routedUsers = userCaptor.getAllValues();
        assertEquals(List.of("sender@example.com", "user2@example.com", "user3@example.com"), routedUsers);
        for (String dest : destCaptor.getAllValues()) {
            assertEquals("/queue/messages", dest);
        }
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

    private void setUsers(GroupChat chat, User[] users) throws Exception {
        Field usersField = GroupChat.class.getDeclaredField("users");
        usersField.setAccessible(true);
        usersField.set(chat, users);
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

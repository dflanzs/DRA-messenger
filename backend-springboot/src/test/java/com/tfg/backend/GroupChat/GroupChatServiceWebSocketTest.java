package com.tfg.backend.GroupChat;

import com.tfg.backend.SignalEnvelope.Message;
import com.tfg.backend.SignalEnvelope.MessageRepository;
import com.tfg.backend.SignalEnvelope.dto.SendMessageDTO;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.lang.reflect.Field;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
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
    private MessageRepository messageRepository;

    @Mock
    private GroupChatRepository groupChatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private TrustCirclesService trustCirclesService;

    private GroupChatService groupChatService;

    @BeforeEach
    void setUp() {
        groupChatService = new GroupChatService(
            messageRepository,
            groupChatRepository,
            userRepository,
            messagingTemplate,
            trustCirclesService
        );
    }

    @Test
    void sendGroupMessage_routesToAllGroupMembersEmails() throws Exception {
        User sender = buildUser(1L, "sender@example.com");
        User user2 = buildUser(2L, "user2@example.com");
        User user3 = buildUser(3L, "user3@example.com");

        GroupChat chat = new GroupChat();
        setUsers(chat, new User[]{sender, user2, user3});

        SendMessageDTO dto = new SendMessageDTO();
        dto.setGroupChatId(22L);
        dto.setContent("mensaje grupo");

        Message persisted = new Message(sender, "mensaje grupo", chat);
        persisted.setId(200L);
        persisted.setCreatedAt(LocalDateTime.of(2026, 4, 9, 12, 30, 0));

        Principal principal = () -> "sender@example.com";

        when(userRepository.findByEmailAndDeletedAtIsNull("sender@example.com"))
            .thenReturn(Optional.of(sender));
        when(groupChatRepository.findById(22L)).thenReturn(Optional.of(chat));
        when(messageRepository.save(any(Message.class))).thenReturn(persisted);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        groupChatService.sendGroupMessage(dto, principal);

        verify(trustCirclesService).validateUsersCanCommunicate(1L, 2L);
        verify(trustCirclesService).validateUsersCanCommunicate(1L, 3L);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, times(3)).convertAndSendToUser(
            userCaptor.capture(),
            org.mockito.Mockito.eq("/queue/messages"),
            any(SendMessageDTO.class)
        );

        List<String> routedUsers = userCaptor.getAllValues();
        assertEquals(List.of("sender@example.com", "user2@example.com", "user3@example.com"), routedUsers);
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

package com.tfg.backend.GroupChat;

import com.tfg.backend.Message.Message;
import com.tfg.backend.Message.MessageRepository;
import com.tfg.backend.Message.dto.SendMessageDTO;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.Principal;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupChatService {

    private final MessageRepository messageRepository;
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TrustCirclesService trustCirclesService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public GroupChatService(
        MessageRepository messageRepository,
        GroupChatRepository groupChatRepository,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate,
        TrustCirclesService trustCirclesService
    ) {
        this.messageRepository = messageRepository;
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.trustCirclesService = trustCirclesService;
    }

    @Transactional
    public void sendGroupMessage(SendMessageDTO messageDTO, Principal senderPrincipal) {
        if (senderPrincipal == null) {
            throw new IllegalArgumentException("Usuario no autenticado");
        }
        User sender = userRepository.findByEmailAndDeletedAtIsNull(senderPrincipal.getName())
            .orElseThrow(() -> new IllegalArgumentException("Remitente no encontrado"));

        GroupChat chat = groupChatRepository.findById(messageDTO.getGroupChatId())
            .orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        Long[] chatUserIds = chat.getUserIds();
        boolean isMember = false;
        for (Long uid : chatUserIds) {
            if (uid.equals(sender.getId())) { isMember = true; break; }
        }
        if (!isMember) {
            throw new IllegalArgumentException("El remitente no pertenece al grupo");
        }

        for (Long recipientId : chatUserIds) {
            if (!recipientId.equals(sender.getId())) {
                trustCirclesService.validateUsersCanCommunicate(sender.getId(), recipientId);
            }
        }

        Message message = new Message(sender, messageDTO.getContent(), chat);
        message.setCreatedAt(LocalDateTime.now());
        Message savedMessage = messageRepository.save(message);

        SendMessageDTO responseDTO = new SendMessageDTO();
        responseDTO.setId(savedMessage.getId());
        responseDTO.setSenderId(sender.getId());
        responseDTO.setContent(savedMessage.getContent());
        responseDTO.setTimestamp(savedMessage.getCreatedAt().format(formatter));
        responseDTO.setRead(false);

        for (int i = 0; i < chatUserIds.length; i++) {
            User userReceiver = userRepository.findById(chatUserIds[i])
                .orElseThrow(() -> new IllegalArgumentException("Usuario receptor no encontrado"));

            messagingTemplate.convertAndSendToUser(userReceiver.getEmail(), "/queue/messages", responseDTO);
        }
    }
}

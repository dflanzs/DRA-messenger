package com.tfg.backend.GroupChat;

import com.tfg.backend.Message.Message;
import com.tfg.backend.Message.MessageRepository;
import com.tfg.backend.Message.dto.SendMessageDTO;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupChatService {

    private final MessageRepository messageRepository;
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public GroupChatService(
        MessageRepository messageRepository,
        GroupChatRepository groupChatRepository,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.messageRepository = messageRepository;
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void sendGroupMessage(SendMessageDTO messageDTO) {
        User sender = userRepository.findById(messageDTO.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("Remitente no encontrado"));

        GroupChat chat = groupChatRepository.findById(messageDTO.getGroupChatId())
            .orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));

        Message message = new Message(sender, messageDTO.getContent(), chat);
        message.setCreatedAt(LocalDateTime.now());
        Message savedMessage = messageRepository.save(message);

        SendMessageDTO responseDTO = new SendMessageDTO();
        responseDTO.setId(savedMessage.getId());
        responseDTO.setSenderId(sender.getId());
        responseDTO.setContent(savedMessage.getContent());
        responseDTO.setTimestamp(savedMessage.getCreatedAt().format(formatter));
        responseDTO.setRead(false);

        messagingTemplate.convertAndSendToUser(sender.getId().toString(), "/queue/messages", responseDTO);
    }
}

package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Message.Message;
import com.tfg.backend.Message.MessageRepository;
import com.tfg.backend.Message.dto.SendMessageDTO;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OneToOneChatService {

    private final MessageRepository messageRepository;
    private final OneToOneChatRepository oneToOneChatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TrustCirclesService trustCirclesService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public OneToOneChatService(
        MessageRepository messageRepository,
        OneToOneChatRepository oneToOneChatRepository,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate,
        TrustCirclesService trustCirclesService
    ) {
        this.messageRepository = messageRepository;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.trustCirclesService = trustCirclesService;
    }

    @Transactional
    public void sendPrivateMessage(SendMessageDTO sendMessageDTO) {
        User sender = userRepository.findById(sendMessageDTO.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("Remitente no encontrado"));

        Long chatId = sendMessageDTO.getOneToOneChatId();
        OneToOneChat chat = oneToOneChatRepository.findById(chatId)
            .orElseThrow(() -> new RuntimeException("Chat no encontrado"));

        Long[] chatUserIds = chat.getUserIds();
        if (!sender.getId().equals(chatUserIds[0]) && !sender.getId().equals(chatUserIds[1])) {
            throw new IllegalArgumentException("El usuario remitente no pertenece al chat");
        }

        Long receiverUserId = sender.getId().equals(chatUserIds[0]) ? chatUserIds[1] : chatUserIds[0];
        trustCirclesService.validateUsersCanCommunicate(sender.getId(), receiverUserId);

        Message message = new Message(sender, sendMessageDTO.getContent(), chat);
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

    public void sendBroadcastMessage(SendMessageDTO sendMessageDTO) {
        User sender = userRepository.findById(sendMessageDTO.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("Remitente no encontrado"));

        SendMessageDTO responseDTO = new SendMessageDTO();
        responseDTO.setSenderId(sender.getId());
        responseDTO.setContent(sendMessageDTO.getContent());
        responseDTO.setTimestamp(LocalDateTime.now().format(formatter));

        messagingTemplate.convertAndSend("/topic/broadcast", responseDTO);
    }
}

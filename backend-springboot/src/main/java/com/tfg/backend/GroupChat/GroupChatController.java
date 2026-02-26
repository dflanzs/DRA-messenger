package com.tfg.backend.GroupChat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.tfg.backend.user.User;
import com.tfg.backend.user.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.tfg.backend.message.*;

@Controller
public class GroupChatController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private GroupChatRepository groupChatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Envía un mensaje privado a un usuario específico
     * Cliente: stompClient.send("/app/private-message", {}, JSON.stringify({senderId, receiverId, content}))
     */
    @MessageMapping("/private-message")
    public void sendPrivateMessage(@Payload MessageDTO messageDTO) {
        Optional<User> senderOpt = userRepository.findById(messageDTO.getSenderId());
        Optional<GroupChat> chatOpt = groupChatRepository.findById(messageDTO.getGroupChatId());

        if (senderOpt.isPresent() && chatOpt.isPresent()) {
            User sender = senderOpt.get();
            GroupChat chat = chatOpt.get();

            // Guardar el mensaje en la BD
            Message message = new Message(sender, messageDTO.getContent(), chat);
            message.setCreatedAt(LocalDateTime.now());
            Message savedMessage = messageRepository.save(message);

            // Preparar DTO para enviar al cliente
            MessageDTO responseDTO = new MessageDTO();
            responseDTO.setId(savedMessage.getId());
            responseDTO.setSenderId(sender.getId());
            responseDTO.setContent(savedMessage.getContent());
            responseDTO.setTimestamp(savedMessage.getCreatedAt().format(formatter));
            responseDTO.setRead(false);

            // Enviar también al sender para confirmación
            messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/queue/messages",
                responseDTO
            );
        }
    }

    /**
     * Envía un mensaje a un broadcast (todo el mundo lo recibe)
     * Cliente: stompClient.send("/app/broadcast-message", {}, JSON.stringify({senderId, content}))
     */
    @MessageMapping("/broadcast-message")
    public void sendBroadcastMessage(@Payload MessageDTO messageDTO) {
        Optional<User> senderOpt = userRepository.findById(messageDTO.getSenderId());

        if (senderOpt.isPresent()) {
            User sender = senderOpt.get();

            MessageDTO responseDTO = new MessageDTO();
            responseDTO.setSenderId(sender.getId());
            responseDTO.setContent(messageDTO.getContent());
            responseDTO.setTimestamp(LocalDateTime.now().format(formatter));

            // Enviar a todos los conectados
            messagingTemplate.convertAndSend("/topic/broadcast", responseDTO);
        }
    }
}

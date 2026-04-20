package com.tfg.backend.Message;

import com.tfg.backend.Message.dto.SendMessageDTO;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.OneToOneChat.OneToOneChatRepository;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OneToOneChatRepository oneToOneChatRepository;
    private final TrustCirclesService trustCirclesService;

    public MessageService(
        MessageRepository messageRepository,
        UserRepository userRepository,
        OneToOneChatRepository oneToOneChatRepository,
        TrustCirclesService trustCirclesService
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.trustCirclesService = trustCirclesService;
    }

    @Transactional(readOnly = true)
    public List<Message> list() {
        return messageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Message getById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        return messageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
    }

    @Transactional
    public Message create(SendMessageDTO messageDTO) {
        User sender = userRepository.findByIdAndDeletedAtIsNull(messageDTO.getSenderId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remitente no válido"));

        Long oneToOneChatOrReceiverId = messageDTO.getOneToOneChatId();
        if (oneToOneChatOrReceiverId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el chat o receptor");
        }

        OneToOneChat chat;
        Long receiverId;

        var existingChat = oneToOneChatRepository.findById(oneToOneChatOrReceiverId);
        if (existingChat.isPresent()) {
            chat = existingChat.get();
            Long[] chatUserIds = chat.getUserIds();
            if (!sender.getId().equals(chatUserIds[0]) && !sender.getId().equals(chatUserIds[1])) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El remitente no pertenece al chat");
            }
            receiverId = sender.getId().equals(chatUserIds[0]) ? chatUserIds[1] : chatUserIds[0];
        } else {
            receiverId = oneToOneChatOrReceiverId;
            User receiver = userRepository.findByIdAndDeletedAtIsNull(receiverId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receptor no válido"));

            chat = oneToOneChatRepository.findChatBetweenUsers(sender.getId(), receiverId)
                .orElseGet(() -> oneToOneChatRepository.save(new OneToOneChat(sender, receiver)));
        }

        trustCirclesService.validateUsersCanCommunicate(sender.getId(), receiverId);

        Message message = new Message(sender, messageDTO.getContent(), chat);
        return messageRepository.save(message);
    }

    @Transactional
    public Message update(Long id, SendMessageDTO messageDTO) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));

        message.setContent(messageDTO.getContent());
        return messageRepository.save(message);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        if (!messageRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado");
        }

        messageRepository.deleteById(id);
    }


    @Transactional(readOnly = true)
    public List<Message> getUnreadMessages(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        List<OneToOneChat> userChats = oneToOneChatRepository.findChatsForUser(userId);

        List<OneToOneChat> allowedChats = userChats.stream()
            .filter(chat -> {
                Long[] userIds = chat.getUserIds();
                Long otherUserId = userId.equals(userIds[0]) ? userIds[1] : userIds[0];
                return trustCirclesService.canUsersCommunicate(userId, otherUserId);
            })
            .toList();

        if (allowedChats.isEmpty()) {
            return List.of();
        }

        return messageRepository.findUnreadMessagesInChats(allowedChats, userId);
    }

    @Transactional
    public Message markAsRead(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        Message message = messageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));

        message.setRead(true);
        return messageRepository.save(message);
    }
}

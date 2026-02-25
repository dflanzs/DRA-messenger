package com.tfg.backend.message;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.user.User;
import com.tfg.backend.user.UserRepository;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Obtener todos los mensajes
     */
    @GetMapping
    public List<Message> list() {
        return messageRepository.findAll();
    }

    /**
     * Obtener un mensaje por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Message> get(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Message> message = messageRepository.findById(id);
        return message.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crear un nuevo mensaje
     */
    @PostMapping
    public ResponseEntity<Message> create(@Valid @RequestBody MessageDTO messageDTO) {
        Optional<User> senderOpt = userRepository.findById(messageDTO.getSenderId());
        
        if (senderOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<User> receiver = userRepository.findById(messageDTO.getOneToOneChatId());
        OneToOneChat chat = new OneToOneChat(senderOpt.get(), receiver.get());
        Message message = new Message(senderOpt.get(), messageDTO.getContent(), chat);
        Message savedMessage = messageRepository.save(message);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedMessage);
    }

    /**
     * Actualizar un mensaje
     */
    @PutMapping("/{id}")
    public ResponseEntity<Message> update(@PathVariable Long id, @Valid @RequestBody MessageDTO messageDTO) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Message> optionalMessage = messageRepository.findById(id);
        if (optionalMessage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = optionalMessage.get();
        message.setContent(messageDTO.getContent());

        return ResponseEntity.ok(messageRepository.save(message));
    }

    /**
     * Eliminar un mensaje
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        if (!messageRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        messageRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener la conversación entre dos usuarios
     */
    @GetMapping("/conversation/{userId1}/{userId2}")
    public ResponseEntity<List<Message>> getConversation(@PathVariable Long userId1, @PathVariable Long userId2) {
        if (userId1 == null || userId2 == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Message> conversation = messageRepository.findConversation(userId1, userId2);
        return ResponseEntity.ok(conversation);
    }

    /**
     * Obtener mensajes no leídos de un usuario
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<Message>> getUnreadMessages(@PathVariable Long userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Message> unreadMessages = messageRepository.findUnreadMessages(userId);
        return ResponseEntity.ok(unreadMessages);
    }

    /**
     * Marcar un mensaje como leído
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Message> markAsRead(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Message> optionalMessage = messageRepository.findById(id);
        if (optionalMessage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Message message = optionalMessage.get();
        message.setRead(true);

        return ResponseEntity.ok(messageRepository.save(message));
    }
}

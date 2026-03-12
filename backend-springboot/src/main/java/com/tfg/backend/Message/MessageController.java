package com.tfg.backend.Message;

import jakarta.validation.Valid;
import java.util.List;
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

import com.tfg.backend.Message.dto.SendMessageDTO;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Obtener todos los mensajes
     */
    @GetMapping
    public List<Message> list() {
        return messageService.list();
    }

    /**
     * Obtener un mensaje por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Message> get(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.getById(id));
    }

    /**
     * Crear un nuevo mensaje
     */
    @PostMapping
    public ResponseEntity<Message> create(@Valid @RequestBody SendMessageDTO messageDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.create(messageDTO));
    }

    /**
     * Actualizar un mensaje
     */
    @PutMapping("/{id}")
    public ResponseEntity<Message> update(@PathVariable Long id, @Valid @RequestBody SendMessageDTO messageDTO) {
        return ResponseEntity.ok(messageService.update(id, messageDTO));
    }

    /**
     * Eliminar un mensaje
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        messageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener la conversación entre dos usuarios
     */
    @GetMapping("/conversation/{userId1}/{userId2}")
    public ResponseEntity<List<Message>> getConversation(@PathVariable Long userId1, @PathVariable Long userId2) {
        return ResponseEntity.ok(messageService.getConversation(userId1, userId2));
    }

    /**
     * Obtener mensajes no leídos de un usuario
     */
    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<Message>> getUnreadMessages(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getUnreadMessages(userId));
    }

    /**
     * Marcar un mensaje como leído
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Message> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(messageService.markAsRead(id));
    }
}

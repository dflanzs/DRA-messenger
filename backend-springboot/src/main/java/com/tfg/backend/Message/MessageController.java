package com.tfg.backend.Message;

import jakarta.validation.Valid;
import java.security.Principal;
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
import org.springframework.web.server.ResponseStatusException;

import com.tfg.backend.Message.dto.SendMessageDTO;
import com.tfg.backend.User.UserService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    public MessageController(MessageService messageService, UserService userService) {
        this.messageService = messageService;
        this.userService = userService;
    }

    /**
     * Obtener todos los mensajes
     */
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public List<Message> list() {
        return messageService.list();
    }

    /**
     * Obtener un mensaje por ID
     */
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
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
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        messageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener mensajes no leídos de un usuario
     */
    @GetMapping("/unread")
    public ResponseEntity<List<Message>> getUnreadMessages(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Usuario no autenticado");
        }
        Long userId = userService.getByEmail(principal.getName()).getId();
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

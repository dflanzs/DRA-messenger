package com.tfg.backend.GroupChat;

import com.tfg.backend.Message.dto.SendMessageDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/group-chats")
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final GroupChatRepository groupChatRepository;

    public GroupChatController(GroupChatService groupChatService,
                               GroupChatRepository groupChatRepository) {
        this.groupChatService = groupChatService;
        this.groupChatRepository = groupChatRepository;
    }

    /**
     * Envía un mensaje a un grupo
     * Cliente: stompClient.send("/app/group-message", {}, JSON.stringify({senderId, groupChatId, content}))
     */
    @MessageMapping("/group-message")
    public void sendGroupMessage(@Payload SendMessageDTO messageDTO,
                                 SimpMessageHeaderAccessor headerAccessor) {
        groupChatService.sendGroupMessage(messageDTO, headerAccessor.getUser());
    }

    @ResponseBody
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public List<GroupChat> list() {
        return groupChatRepository.findAll();
    }

    @ResponseBody
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!groupChatRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        groupChatRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

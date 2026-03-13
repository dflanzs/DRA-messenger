package com.tfg.backend.GroupChat;

import com.tfg.backend.Message.dto.SendMessageDTO;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class GroupChatController {

    private final GroupChatService groupChatService;

    public GroupChatController(GroupChatService groupChatService) {
        this.groupChatService = groupChatService;
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
}

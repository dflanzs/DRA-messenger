package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Message.dto.SendMessageDTO;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class OneToOneChatController {

	private final OneToOneChatService oneToOneChatService;

	public OneToOneChatController(OneToOneChatService oneToOneChatService) {
		this.oneToOneChatService = oneToOneChatService;
	}

	/**
	 * Envía un mensaje privado a un usuario específico
	 * Cliente: stompClient.send("/app/private-message", {}, JSON.stringify({senderId, receiverId, content}))
	 */
	@MessageMapping("/private-message")
	public void sendPrivateMessage(@Payload SendMessageDTO sendMessageDTO,
	                               SimpMessageHeaderAccessor headerAccessor) {
		oneToOneChatService.sendPrivateMessage(sendMessageDTO, headerAccessor.getUser());
	}
}

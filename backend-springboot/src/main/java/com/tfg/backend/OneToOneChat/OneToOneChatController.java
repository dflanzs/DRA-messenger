package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Message.dto.SendMessageDTO;
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
	public void sendPrivateMessage(@Payload SendMessageDTO sendMessageDTO) {
		oneToOneChatService.sendPrivateMessage(sendMessageDTO);
	}

	/**
	 * Envía un mensaje a un broadcast (todo el mundo lo recibe)
	 * Cliente: stompClient.send("/app/broadcast-message", {}, JSON.stringify({senderId, content}))
	 */
	@MessageMapping("/broadcast-message")
	public void sendBroadcastMessage(@Payload SendMessageDTO sendMessageDTO) {
		oneToOneChatService.sendBroadcastMessage(sendMessageDTO);
	}
}

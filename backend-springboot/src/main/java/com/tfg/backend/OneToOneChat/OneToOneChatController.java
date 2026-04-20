package com.tfg.backend.OneToOneChat;

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
@RequestMapping("/api/private-chats")
public class OneToOneChatController {

	private final OneToOneChatService oneToOneChatService;
	private final OneToOneChatRepository oneToOneChatRepository;

	public OneToOneChatController(OneToOneChatService oneToOneChatService,
	                              OneToOneChatRepository oneToOneChatRepository) {
		this.oneToOneChatService = oneToOneChatService;
		this.oneToOneChatRepository = oneToOneChatRepository;
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

	@ResponseBody
	@PreAuthorize("@authorizationService.isAdmin(authentication)")
	@GetMapping
	public List<OneToOneChat> list() {
		return oneToOneChatRepository.findAll();
	}

	@ResponseBody
	@PreAuthorize("@authorizationService.isAdmin(authentication)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		if (!oneToOneChatRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		oneToOneChatRepository.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}

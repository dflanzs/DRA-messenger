package com.tfg.backend.OneToOneChat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.tfg.backend.Message.*;
import com.tfg.backend.Message.dto.SendMessageDTO;

@Controller
public class OneToOneChatController {

	@Autowired
	private MessageRepository messageRepository;

	@Autowired
	private OneToOneChatRepository oneToOneChatRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private TrustCirclesService trustCirclesService;

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

	/**
	 * Envía un mensaje privado a un usuario específico
	 * Cliente: stompClient.send("/app/private-message", {}, JSON.stringify({senderId, receiverId, content}))
	 */
	@MessageMapping("/private-message")
	public void sendPrivateMessage(@Payload SendMessageDTO sendMessageDTO) {
		Optional<User> senderOpt = userRepository.findById(sendMessageDTO.getSenderId());

		Long receiverId = sendMessageDTO.getOneToOneChatId();
		Optional<OneToOneChat> chatOpt = oneToOneChatRepository.findById(receiverId);
		OneToOneChat chat = chatOpt.orElseThrow(() -> new RuntimeException("Chat no encontrado"));

		if (senderOpt.isPresent()) {
			User sender = senderOpt.get();
			Long[] chatUserIds = chat.getUserIds();
			if (!sender.getId().equals(chatUserIds[0]) && !sender.getId().equals(chatUserIds[1])) {
				throw new IllegalArgumentException("El usuario remitente no pertenece al chat");
			}

			Long receiverUserId = sender.getId().equals(chatUserIds[0]) ? chatUserIds[1] : chatUserIds[0];
			trustCirclesService.validateUsersCanCommunicate(sender.getId(), receiverUserId);

			// Guardar el mensaje en la BD
			Message message = new Message(sender, sendMessageDTO.getContent(), chat);
			message.setCreatedAt(LocalDateTime.now());
			Message savedMessage = messageRepository.save(message);

			// Preparar DTO para enviar al cliente
			SendMessageDTO responseDTO = new SendMessageDTO();
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
	public void sendBroadcastMessage(@Payload SendMessageDTO sendMessageDTO) {
		Optional<User> senderOpt = userRepository.findById(sendMessageDTO.getSenderId());

		if (senderOpt.isPresent()) {
			User sender = senderOpt.get();

			SendMessageDTO responseDTO = new SendMessageDTO();
			responseDTO.setSenderId(sender.getId());
			responseDTO.setContent(sendMessageDTO.getContent());
			responseDTO.setTimestamp(LocalDateTime.now().format(formatter));

			// Enviar a todos los conectados
			messagingTemplate.convertAndSend("/topic/broadcast", responseDTO);
		}
	}
}

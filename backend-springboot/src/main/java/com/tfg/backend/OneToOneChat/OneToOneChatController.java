package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Chat.dto.CreateDirectChatRequestDto;
import com.tfg.backend.Chat.dto.DirectChatResultDto;
import com.tfg.backend.Chat.dto.DirectChatSummaryDto;
import com.tfg.backend.TrustCircles.CommunicationRequest;
import com.tfg.backend.TrustCircles.CommunicationRequestService;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.TrustCircles.dto.CommunicationRequestDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import java.util.List;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/private-chats")
public class OneToOneChatController {

	private final OneToOneChatRepository oneToOneChatRepository;
	private final UserService userService;
	private final TrustCirclesService trustCirclesService;
	private final CommunicationRequestService communicationRequestService;

    public OneToOneChatController(
        OneToOneChatRepository oneToOneChatRepository,
        UserService userService,
        TrustCirclesService trustCirclesService,
        CommunicationRequestService communicationRequestService
    ) {
		this.oneToOneChatRepository = oneToOneChatRepository;
		this.userService = userService;
		this.trustCirclesService = trustCirclesService;
		this.communicationRequestService = communicationRequestService;
	}


	@ResponseBody
	@PreAuthorize("@authorizationService.isAdmin(authentication)")
	@GetMapping
	public List<OneToOneChat> list() {
		return oneToOneChatRepository.findAll();
	}

	@ResponseBody
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/me")
	public List<DirectChatSummaryDto> listForCurrentUser(Principal principal) {
		Long currentUserId = userService.getByEmail(principal.getName()).getId();
		return oneToOneChatRepository.findChatsForUser(currentUserId).stream()
			.map(chat -> {
				Long otherUserId = chat.getUser1().getId().equals(currentUserId)
					? chat.getUser2().getId()
					: chat.getUser1().getId();
				User otherUser = userService.getById(otherUserId);
				return new DirectChatSummaryDto(
					chat.getId(),
					chat.getUser1().getId(),
					chat.getUser2().getId(),
					otherUser.getName(),
					chat.getCreatedAt()
				);
			})
			.toList();
	}

	@ResponseBody
	@PreAuthorize("isAuthenticated()")
	@PostMapping
	public ResponseEntity<DirectChatResultDto> createOrGetChat(
		@RequestBody CreateDirectChatRequestDto request,
		Principal principal
	) {
		User currentUser = userService.getByEmail(principal.getName());
		User targetUser = userService.getById(request.targetUserId());
		if (currentUser.getId().equals(targetUser.getId())) {
			return ResponseEntity.badRequest().build();
		}

		// Si los usuarios ya comparten círculo de confianza, el chat se crea
		// directamente. Si no, se genera una solicitud de comunicación pendiente.
		if (trustCirclesService.canUsersCommunicate(currentUser.getId(), targetUser.getId())) {
			OneToOneChat existing = oneToOneChatRepository.findChatBetweenUsers(currentUser.getId(), targetUser.getId());
			OneToOneChat chat = existing != null ? existing : oneToOneChatRepository.save(new OneToOneChat(currentUser, targetUser));
			DirectChatSummaryDto chatDto = new DirectChatSummaryDto(
				chat.getId(),
				chat.getUser1().getId(),
				chat.getUser2().getId(),
				targetUser.getName(),
				chat.getCreatedAt()
			);
			return ResponseEntity.ok(new DirectChatResultDto("ACTIVE", chatDto, null));
		}

		CommunicationRequest commRequest =
			communicationRequestService.createRequest(currentUser.getId(), targetUser.getId());
		CommunicationRequestDto requestDto = new CommunicationRequestDto(
			commRequest.getId(),
			commRequest.getRequester().getId(),
			commRequest.getRequester().getName(),
			commRequest.getTarget().getId(),
			commRequest.getTarget().getName(),
			commRequest.getCreatedAt()
		);
		return ResponseEntity.ok(new DirectChatResultDto("PENDING", null, requestDto));
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

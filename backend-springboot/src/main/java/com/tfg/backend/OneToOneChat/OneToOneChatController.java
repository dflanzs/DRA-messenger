package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Chat.dto.CreateDirectChatRequestDto;
import com.tfg.backend.Chat.dto.DirectChatResultDto;
import com.tfg.backend.Chat.dto.DirectChatSummaryDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import java.util.List;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/private-chats")
public class OneToOneChatController {

	private final OneToOneChatRepository oneToOneChatRepository;
	private final OneToOneChatService oneToOneChatService;
	private final UserService userService;

    public OneToOneChatController(
        OneToOneChatRepository oneToOneChatRepository,
        OneToOneChatService oneToOneChatService,
        UserService userService
    ) {
		this.oneToOneChatRepository = oneToOneChatRepository;
		this.oneToOneChatService = oneToOneChatService;
		this.userService = userService;
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
		return ResponseEntity.ok(oneToOneChatService.createOrGetChat(request, principal));
	}

	@ResponseBody
	@PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #id)")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
		oneToOneChatService.delete(id, principal);
		return ResponseEntity.noContent().build();
	}
}

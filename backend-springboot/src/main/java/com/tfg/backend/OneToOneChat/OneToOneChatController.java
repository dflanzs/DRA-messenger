package com.tfg.backend.OneToOneChat;

import java.util.List;

import org.springframework.http.ResponseEntity;
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

	private final OneToOneChatRepository oneToOneChatRepository;

    public OneToOneChatController(
        OneToOneChatRepository oneToOneChatRepository
    ) {
		this.oneToOneChatRepository = oneToOneChatRepository;
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

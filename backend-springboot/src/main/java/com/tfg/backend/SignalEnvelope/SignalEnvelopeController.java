package com.tfg.backend.SignalEnvelope;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.backend.GroupChat.GroupChatService;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/messages")
public class SignalEnvelopeController {

    private final SignalEnvelopeService messageService;
    private final UserService userService;
    private final TrustCirclesService trustCirclesService;

    public SignalEnvelopeController(
            SignalEnvelopeService messageService,
            UserService userService,
            TrustCirclesService trustCirclesService
            ) {
        this.messageService = messageService;
        this.userService = userService;
        this.trustCirclesService = trustCirclesService;
    }

    @MessageMapping("/group-message")
    public void sendGroupMessage(
            @Payload SignalGroupMessageRequestDto messageDTO,
            Principal principal
    ) {
        User sender = userService.getByEmail(principal.getName());

        messageService.sendGroupMessage(sender.getId(), messageDTO);
    }

	@MessageMapping("/private-message")
	public void sendPrivateMessage(
            @Valid SignalDirectMessageRequestDto request,
            Principal principal
    ) {
        // Check if sender can communicate with recipient
        User sender = userService.getByEmail(principal.getName());

        if (!trustCirclesService.canUsersCommunicate(request.getRecipientUserId(), sender.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Can not communicate with this user");
        }

		messageService.sendPrivateMessage(sender.getId(), request);
	}
    
    @GetMapping("/pending")
    public List<SignalEnvelope> getPendingMessages(Principal principal) {
        User user = userService.getByEmail(principal.getName());
        return messageService.getPendingMessages(user.getId());
    }
    
}

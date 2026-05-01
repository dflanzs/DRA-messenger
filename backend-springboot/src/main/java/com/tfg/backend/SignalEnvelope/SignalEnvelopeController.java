package com.tfg.backend.SignalEnvelope;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageResponseDto;
import com.tfg.backend.SignalEnvelope.dto.SignalMessageWSDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageResponseDto;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;

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
    public SignalGroupMessageResponseDto sendGroupMessage(
            @Payload SignalGroupMessageRequestDto messageDTO,
            Principal principal
    ) {
        User sender = userService.getByEmail(principal.getName());

        SignalGroupMessageResponseDto response = messageService.sendGroupMessage(sender.getId(), messageDTO);
        return response;
    }

	@MessageMapping("/private-message")
	public SignalDirectMessageResponseDto sendPrivateMessage(
            @Valid SignalDirectMessageRequestDto request,
            Principal principal
    ) {
        // Check if sender can communicate with recipient
        User sender = userService.getByEmail(principal.getName());

        if (!trustCirclesService.canUsersCommunicate(request.getRecipientUserId(), sender.getId())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Can not communicate with this user");
        }

		SignalDirectMessageResponseDto response = messageService.sendPrivateMessage(sender.getId(), request);
        return response;
	}
    
    @GetMapping("/pending")
    public List<SignalMessageWSDto> getPendingGroupMessages(Principal principal) {
        User user = userService.getByEmail(principal.getName());
        if (user == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found");
        }

        return messageService.getPendingMessages(user.getId());
    }
}

package com.tfg.backend.TrustCircles;

import com.tfg.backend.Chat.dto.DirectChatSummaryDto;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.TrustCircles.dto.CommunicationRequestDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/communication-requests")
public class CommunicationRequestController {

    private final CommunicationRequestService communicationRequestService;
    private final UserService userService;

    public CommunicationRequestController(
        CommunicationRequestService communicationRequestService,
        UserService userService
    ) {
        this.communicationRequestService = communicationRequestService;
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/incoming")
    public List<CommunicationRequestDto> incoming(Principal principal) {
        Long userId = userService.getByEmail(principal.getName()).getId();
        return communicationRequestService.listIncoming(userId).stream()
            .map(this::toDto)
            .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/outgoing")
    public List<CommunicationRequestDto> outgoing(Principal principal) {
        Long userId = userService.getByEmail(principal.getName()).getId();
        return communicationRequestService.listOutgoing(userId).stream()
            .map(this::toDto)
            .toList();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/accept")
    public DirectChatSummaryDto accept(@PathVariable Long id, Principal principal) {
        Long userId = userService.getByEmail(principal.getName()).getId();
        OneToOneChat chat = communicationRequestService.accept(id, userId);

        Long otherUserId = chat.getUser1().getId().equals(userId)
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
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long id, Principal principal) {
        Long userId = userService.getByEmail(principal.getName()).getId();
        communicationRequestService.reject(id, userId);
    }

    private CommunicationRequestDto toDto(CommunicationRequest request) {
        return new CommunicationRequestDto(
            request.getId(),
            request.getRequester().getId(),
            request.getRequester().getName(),
            request.getTarget().getId(),
            request.getTarget().getName(),
            request.getCreatedAt()
        );
    }
}

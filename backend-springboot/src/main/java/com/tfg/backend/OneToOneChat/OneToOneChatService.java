package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Audit.AuditService;
import com.tfg.backend.Chat.dto.CreateDirectChatRequestDto;
import com.tfg.backend.Chat.dto.DirectChatResultDto;
import com.tfg.backend.Chat.dto.DirectChatSummaryDto;
import com.tfg.backend.Enums.AuditAction;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.TrustCircles.CommunicationRequest;
import com.tfg.backend.TrustCircles.CommunicationRequestService;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.TrustCircles.dto.CommunicationRequestDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OneToOneChatService {

    private final OneToOneChatRepository oneToOneChatRepository;
    private final UserService userService;
    private final SignalEnvelopeRepository signalEnvelopeRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final TrustCirclesService trustCirclesService;
    private final CommunicationRequestService communicationRequestService;
    private final AuditService auditService;

    public OneToOneChatService(
        OneToOneChatRepository oneToOneChatRepository,
        UserService userService,
        SignalEnvelopeRepository signalEnvelopeRepository,
        SimpMessagingTemplate simpMessagingTemplate,
        TrustCirclesService trustCirclesService,
        CommunicationRequestService communicationRequestService,
        AuditService auditService
    ) {
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.userService = userService;
        this.signalEnvelopeRepository = signalEnvelopeRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.trustCirclesService = trustCirclesService;
        this.communicationRequestService = communicationRequestService;
        this.auditService = auditService;
    }

    @Transactional
    public DirectChatResultDto createOrGetChat(CreateDirectChatRequestDto request, Principal principal) {
        User currentUser = userService.getByEmail(principal.getName());
        User targetUser = userService.getById(request.targetUserId());
        if (currentUser.getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes crear un chat contigo mismo");
        }

        // Si los usuarios ya comparten círculo de confianza, el chat se crea
        // directamente. Si no, se genera una solicitud de comunicación pendiente.
        if (trustCirclesService.canUsersCommunicate(currentUser.getId(), targetUser.getId())) {
            OneToOneChat existing = oneToOneChatRepository.findChatBetweenUsers(currentUser.getId(), targetUser.getId());
            OneToOneChat chat;
            if (existing != null) {
                chat = existing;
            } else {
                chat = oneToOneChatRepository.save(new OneToOneChat(currentUser, targetUser));
                auditService.record(AuditAction.CREATE_OTO_CHAT, currentUser.getId());
            }
            DirectChatSummaryDto chatDto = new DirectChatSummaryDto(
                chat.getId(),
                chat.getUser1().getId(),
                chat.getUser2().getId(),
                targetUser.getName(),
                chat.getCreatedAt()
            );
            return new DirectChatResultDto("ACTIVE", chatDto, null);
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
        return new DirectChatResultDto("PENDING", null, requestDto);
    }

    @Transactional
    public void delete(Long id, Principal principal) {
        if (!oneToOneChatRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat no encontrado");
        }

        Long currentUserId = userService.getByEmail(principal.getName()).getId();
        Long[] userIds = oneToOneChatRepository.findById(id).get().getUserIds();

        if (!userIds[0].equals(currentUserId) && !userIds[1].equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar este chat");
        }

        oneToOneChatRepository.deleteById(id);
        auditService.record(AuditAction.DELETE_OTO_CHAT, currentUserId);
    }
}

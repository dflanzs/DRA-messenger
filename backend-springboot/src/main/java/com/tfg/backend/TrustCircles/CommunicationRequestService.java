package com.tfg.backend.TrustCircles;

import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.OneToOneChat.OneToOneChatRepository;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CommunicationRequestService {

    private final CommunicationRequestRepository requestRepository;
    private final TrustCirclesService trustCirclesService;
    private final OneToOneChatRepository oneToOneChatRepository;
    private final UserService userService;

    public CommunicationRequestService(
        CommunicationRequestRepository requestRepository,
        TrustCirclesService trustCirclesService,
        OneToOneChatRepository oneToOneChatRepository,
        UserService userService
    ) {
        this.requestRepository = requestRepository;
        this.trustCirclesService = trustCirclesService;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.userService = userService;
    }

    /**
     * Crea una solicitud de comunicación pendiente. Si ya existe una pendiente
     * entre ambos usuarios (en cualquier dirección), la devuelve sin duplicar.
     */
    @Transactional
    public CommunicationRequest createRequest(Long requesterId, Long targetId) {
        if (requesterId == null || targetId == null || requesterId.equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solicitud de comunicación inválida");
        }

        User requester = userService.getById(requesterId);
        User target = userService.getById(targetId);

        return requestRepository
            .findBetweenUsersWithStatus(requesterId, targetId, CommunicationRequestStatus.PENDING)
            .orElseGet(() -> requestRepository.save(new CommunicationRequest(requester, target)));
    }

    /**
     * El target acepta la solicitud: se crea el consent domain entre ambos
     * usuarios y el chat directo. Devuelve el chat creado (o el ya existente).
     */
    @Transactional
    public OneToOneChat accept(Long requestId, Long currentUserId) {
        CommunicationRequest request = loadPendingForTarget(requestId, currentUserId);

        Long requesterId = request.getRequester().getId();
        Long targetId = request.getTarget().getId();

        trustCirclesService.grantCrossCircleConsent(requesterId, targetId);

        OneToOneChat existing = oneToOneChatRepository.findChatBetweenUsers(requesterId, targetId);
        OneToOneChat chat = existing != null
            ? existing
            : oneToOneChatRepository.save(new OneToOneChat(request.getRequester(), request.getTarget()));

        request.setStatus(CommunicationRequestStatus.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);

        return chat;
    }

    /**
     * El target rechaza la solicitud.
     */
    @Transactional
    public void reject(Long requestId, Long currentUserId) {
        CommunicationRequest request = loadPendingForTarget(requestId, currentUserId);
        request.setStatus(CommunicationRequestStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public List<CommunicationRequest> listIncoming(Long userId) {
        return requestRepository.findByTargetIdAndStatus(userId, CommunicationRequestStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<CommunicationRequest> listOutgoing(Long userId) {
        return requestRepository.findByRequesterIdAndStatus(userId, CommunicationRequestStatus.PENDING);
    }

    private CommunicationRequest loadPendingForTarget(Long requestId, Long currentUserId) {
        CommunicationRequest request = requestRepository.findById(requestId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (request.getStatus() != CommunicationRequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya no está pendiente");
        }
        if (!request.getTarget().getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Solo el destinatario puede responder a la solicitud");
        }
        return request;
    }
}

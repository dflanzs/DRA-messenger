package com.tfg.backend.OneToOneChat;

import com.tfg.backend.Cypher.Entity.SignalEnvelope;
import com.tfg.backend.Cypher.Repositories.SignalEnvelopeRepository;
import com.tfg.backend.Cypher.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageResponseDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageWSDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;

import java.util.Base64;
import java.util.Base64.Decoder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OneToOneChatService {

    private final OneToOneChatRepository oneToOneChatRepository;
    private final UserService userService;
    private final SignalEnvelopeRepository signalEnvelopeRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public OneToOneChatService(
        OneToOneChatRepository oneToOneChatRepository,
        UserService userService,
        SignalEnvelopeRepository signalEnvelopeRepository,
        SimpMessagingTemplate simpMessagingTemplate
    ) {
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.userService = userService;
        this.signalEnvelopeRepository = signalEnvelopeRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Transactional
    public SignalDirectMessageResponseDto sendPrivateMessage(Long senderUserId, SignalDirectMessageRequestDto request) {
        // Check if recipient exists
        User recipient = userService.getById(request.getRecipientUserId());
        if (recipient == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Recipient user does not exist");
        }

        // Check if conversationId corresponds to a valid one-to-one chat between sender and recipient
        OneToOneChat oneToOneChat = oneToOneChatRepository.findChatBetweenUsers(senderUserId, request.getRecipientUserId());
        if (request.getConversationId() != oneToOneChat.getId()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid conversation ID");
        }

        User sender = userService.getById(senderUserId);

        Decoder decoder = java.util.Base64.getDecoder();
        SignalEnvelope signalEnvelope = new SignalEnvelope(
                sender,
                recipient,
                oneToOneChat,
                decoder.decode(request.getCypherTextB64()),
                request.getCypherTextType()
                );
        
        if (signalEnvelope != null && signalEnvelope.IsConversationConsistent()) {
            signalEnvelopeRepository.save(signalEnvelope);
        }

        // Send message through WebSocket to recipient
        SignalDirectMessageWSDto wsMessage = new SignalDirectMessageWSDto(
                    signalEnvelope.getId(),
                    signalEnvelope,
                    signalEnvelope.getSender().getId(),
                    signalEnvelope.getReceiver().getId(),
                    signalEnvelope.getConversationType(),
                    request.getCypherTextType(),
                    Base64.getEncoder().encodeToString(signalEnvelope.getCypherText()),
                    signalEnvelope.getCreatedAt()
                );
        
        simpMessagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/signal-messages", wsMessage);

        return new SignalDirectMessageResponseDto(signalEnvelope.getId(), signalEnvelope.getCreatedAt());
    }
}

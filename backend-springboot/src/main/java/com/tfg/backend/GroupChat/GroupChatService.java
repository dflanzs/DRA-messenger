package com.tfg.backend.GroupChat;

import com.tfg.backend.Cypher.Entity.SignalEnvelope;
import com.tfg.backend.Cypher.Repositories.SignalEnvelopeRepository;
import com.tfg.backend.Cypher.dto.SignalDirectMessageWSDto;
import com.tfg.backend.Cypher.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.Cypher.dto.SignalGroupMessageResponseDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Base64.Decoder;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GroupChatService {

    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final SignalEnvelopeRepository signalEnvelopeRepository;

    public GroupChatService(
        GroupChatRepository groupChatRepository,
        UserRepository userRepository,
        SimpMessagingTemplate messagingTemplate,
        UserService userService,
        SignalEnvelopeRepository signalEnvelopeRepository
    ) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.signalEnvelopeRepository = signalEnvelopeRepository;
    }

    @Transactional
    public SignalGroupMessageResponseDto sendGroupMessage(Long senderUserId, SignalGroupMessageRequestDto request) {
        // Check if conversationId corresponds to a valid group chat the sender is part of
        if (!groupChatRepository.existsByIdAndUser_Id(request.getGroupChatId(), senderUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid conversation ID or sender is not part of the group chat");
        }

        User sender = userService.getById(senderUserId);

        GroupChat groupChat = groupChatRepository.findById(request.getGroupChatId())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Group chat not found"));
        Set<Long> chatUserIds = groupChat.getUserIds();

        List<Long> envelopeIds = new ArrayList<>();

        for (Long id : chatUserIds) {
            User recipient = userService.getById(id);
            Decoder decoder = java.util.Base64.getDecoder();
            SignalEnvelope signalEnvelope = new SignalEnvelope(
                    sender,
                    recipient,
                    groupChat,
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
            User userReceiver = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario receptor no encontrado"));

            messagingTemplate.convertAndSendToUser(userReceiver.getEmail(), "/queue/messages", wsMessage);

            envelopeIds.add(signalEnvelope.getId());
        }

        return new SignalGroupMessageResponseDto(envelopeIds, LocalDateTime.now());
    }
}

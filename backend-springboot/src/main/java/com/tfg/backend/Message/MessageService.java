package com.tfg.backend.Message;

import com.tfg.backend.Cypher.Entity.SignalEnvelope;
import com.tfg.backend.Cypher.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageResponseDto;
import com.tfg.backend.Cypher.dto.SignalDirectMessageWSDto;
import com.tfg.backend.Cypher.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.Cypher.dto.SignalGroupMessageResponseDto;
import com.tfg.backend.GroupChat.GroupChat;
import com.tfg.backend.GroupChat.GroupChatRepository;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.OneToOneChat.OneToOneChatRepository;
import com.tfg.backend.TrustCircles.TrustCirclesService;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.Cypher.Repositories.SignalEnvelopeRepository;
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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final OneToOneChatRepository oneToOneChatRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final SignalEnvelopeRepository signalEnvelopeRepository;
    private final GroupChatRepository groupChatRepository;

    public MessageService(
        MessageRepository messageRepository,
        UserRepository userRepository,
        OneToOneChatRepository oneToOneChatRepository,
        TrustCirclesService trustCirclesService,
        SimpMessagingTemplate messagingTemplate,
        UserService userService,
        SignalEnvelopeRepository signalEnvelopeRepository,
        GroupChatRepository groupChatRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.trustCirclesService = trustCirclesService;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.signalEnvelopeRepository = signalEnvelopeRepository;
        this.groupChatRepository = groupChatRepository;
    }

    @Transactional(readOnly = true)
    public List<Message> list() {
        return messageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Message getById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        return messageRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
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

        Decoder decoder = java.util.Base64.getDecoder();
        for (Long id : chatUserIds) {
            User recipient = userService.getById(id);
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
        
        messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/signal-messages", wsMessage);

        return new SignalDirectMessageResponseDto(signalEnvelope.getId(), signalEnvelope.getCreatedAt());
    }
}

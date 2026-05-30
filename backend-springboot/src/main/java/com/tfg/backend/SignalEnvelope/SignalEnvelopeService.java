package com.tfg.backend.SignalEnvelope;

import com.tfg.backend.GroupChat.GroupChat;
import com.tfg.backend.GroupChat.GroupChatRepository;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.OneToOneChat.OneToOneChatRepository;
import com.tfg.backend.SignalEnvelope.SignalEnvelope.MessageStatus;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageResponseDto;
import com.tfg.backend.SignalEnvelope.dto.SignalMessageWSDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageResponseDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupSenderKeyRequestDto;
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

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class SignalEnvelopeService {

    private static final Logger logger = LoggerFactory.getLogger(SignalEnvelopeService.class);

    private final UserRepository userRepository;
    private final OneToOneChatRepository oneToOneChatRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;
    private final SignalEnvelopeRepository signalEnvelopeRepository;
    private final GroupChatRepository groupChatRepository;

    public SignalEnvelopeService(
        UserRepository userRepository,
        OneToOneChatRepository oneToOneChatRepository,
        SimpMessagingTemplate messagingTemplate,
        UserService userService,
        SignalEnvelopeRepository signalEnvelopeRepository,
        GroupChatRepository groupChatRepository
    ) {
        this.userRepository = userRepository;
        this.oneToOneChatRepository = oneToOneChatRepository;
        this.messagingTemplate = messagingTemplate;
        this.userService = userService;
        this.signalEnvelopeRepository = signalEnvelopeRepository;
        this.groupChatRepository = groupChatRepository;
    }

    @Transactional(readOnly = true)
    public SignalEnvelope getById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        return signalEnvelopeRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
    }

    @Transactional
    public SignalGroupMessageResponseDto sendGroupMessage(Long senderUserId, SignalGroupMessageRequestDto request) {
        // Check if conversationId corresponds to a valid group chat the sender is part of
        if (!groupChatRepository.existsByIdAndUsers_Id(request.getGroupChatId(), senderUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid conversation ID or sender is not part of the group chat");
        }

        User sender = userService.getById(senderUserId);

        GroupChat groupChat = groupChatRepository.findById(request.getGroupChatId())
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Group chat not found"));
        Set<Long> chatUserIds = groupChat.getUserIds();

        List<Long> envelopeIds = new ArrayList<>();

        Decoder decoder = java.util.Base64.getDecoder();
        for (Long id : chatUserIds) {
            if (id.equals(senderUserId)) {
                // El emisor guarda su propio mensaje en local; no se persiste sobre
                // ni se hace push para él (evita duplicado al drenar pendientes).
                continue;
            }
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
            SignalMessageWSDto wsMessage = new SignalMessageWSDto(
                    signalEnvelope.getId(),
                    signalEnvelope.getSender().getId(),
                    groupChat.getId(),
                    signalEnvelope.getConversationType(),
                    request.getCypherTextType(),
                    Base64.getEncoder().encodeToString(signalEnvelope.getCypherText()),
                    signalEnvelope.getCreatedAt()
                    );
            User userReceiver = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario receptor no encontrado"));

            // Misma cola que los privados (/queue/signal-messages): la app solo está
            // suscrita ahí y el handler ya sabe enrutar por conversationType.
            logger.info("WS push grupo -> email={}, convId={}, envelopeId={}, dest=/queue/signal-messages",
                    userReceiver.getEmail(), wsMessage.getConversationId(), wsMessage.getEnvelopeId());
            try {
                messagingTemplate.convertAndSendToUser(userReceiver.getEmail(), "/queue/signal-messages", wsMessage);
                logger.info("WS push grupo OK -> {}", userReceiver.getEmail());
            } catch (Exception ex) {
                logger.error("WS push grupo FALLO -> {}", userReceiver.getEmail(), ex);
            }

            envelopeIds.add(signalEnvelope.getId());
        }

        return new SignalGroupMessageResponseDto(envelopeIds, LocalDateTime.now());
    }

    @Transactional
    public void sendGroupSenderKey(Long senderUserId, SignalGroupSenderKeyRequestDto request) {
        // Emisor y receptor deben pertenecer al grupo.
        if (!groupChatRepository.existsByIdAndUsers_Id(request.getGroupChatId(), senderUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sender is not part of the group chat");
        }
        if (!groupChatRepository.existsByIdAndUsers_Id(request.getGroupChatId(), request.getRecipientUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient is not part of the group chat");
        }

        GroupChat groupChat = groupChatRepository.findById(request.getGroupChatId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group chat not found"));

        User sender = userService.getById(senderUserId);
        User recipient = userService.getById(request.getRecipientUserId());

        Decoder decoder = java.util.Base64.getDecoder();
        SignalEnvelope envelope = SignalEnvelope.senderKey(
                sender,
                recipient,
                groupChat,
                decoder.decode(request.getCypherTextB64()),
                request.getCypherTextType()
                );

        if (envelope.IsConversationConsistent()) {
            signalEnvelopeRepository.save(envelope);
        }

        // El SKDM va cifrado 1:1: el receptor lo descifra con decryptDirect y enruta por DirectFrame.
        SignalMessageWSDto wsMessage = new SignalMessageWSDto(
                envelope.getId(),
                envelope.getSender().getId(),
                groupChat.getId(),
                envelope.getConversationType(),
                request.getCypherTextType(),
                Base64.getEncoder().encodeToString(envelope.getCypherText()),
                envelope.getCreatedAt()
                );

        logger.info("WS push sender-key -> email={}, groupId={}, envelopeId={}",
                recipient.getEmail(), groupChat.getId(), wsMessage.getEnvelopeId());
        try {
            messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/signal-messages", wsMessage);
            logger.info("WS push sender-key OK -> {}", recipient.getEmail());
        } catch (Exception ex) {
            logger.error("WS push sender-key FALLO -> {}", recipient.getEmail(), ex);
        }
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
        if (oneToOneChat == null || !oneToOneChat.getId().equals(request.getConversationId())) {
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
        SignalMessageWSDto wsMessage = new SignalMessageWSDto(
                    signalEnvelope.getId(),
                    signalEnvelope.getSender().getId(),
                    oneToOneChat.getId(),
                    signalEnvelope.getConversationType(),
                    request.getCypherTextType(),
                    Base64.getEncoder().encodeToString(signalEnvelope.getCypherText()),
                    signalEnvelope.getCreatedAt()
                );
        
        logger.info("WS push privado -> email={}, convId={}, envelopeId={}, senderId={}",
                recipient.getEmail(), wsMessage.getConversationId(), wsMessage.getEnvelopeId(), wsMessage.getSenderUserId());
        try {
            messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/signal-messages", wsMessage);
            logger.info("WS push privado OK -> {}", recipient.getEmail());
        } catch (Exception ex) {
            logger.error("WS push privado FALLO -> {}", recipient.getEmail(), ex);
        }

        return new SignalDirectMessageResponseDto(signalEnvelope.getId(), signalEnvelope.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<SignalMessageWSDto> getPendingMessages(Long userId) {
        List<SignalEnvelope> pendingMessages = signalEnvelopeRepository.findByReceiver_IdAndStatus(userId, MessageStatus.PENDING);

        List<SignalMessageWSDto> response = new ArrayList<>();

        for (SignalEnvelope envelope : pendingMessages) {
            // GROUP y SENDER_KEY van atados al grupo (group_chat_id); el receptor distingue
            // por conversationType: GROUP -> decryptGroup, SENDER_KEY -> decryptDirect + DirectFrame.
            if (envelope.getConversationType().equals(SignalEnvelope.ConversationType.GROUP.getValue())
                    || envelope.getConversationType().equals(SignalEnvelope.ConversationType.SENDER_KEY.getValue())) {
                response.add(
                        new SignalMessageWSDto(
                            envelope.getId(),
                            envelope.getSender().getId(),
                            envelope.getGroupChat().getId(),
                            envelope.getConversationType(),
                            envelope.getCypherTextType(),
                            Base64.getEncoder().encodeToString(envelope.getCypherText()),
                            envelope.getCreatedAt()
                        ));
            } else if (envelope.getConversationType().equals(SignalEnvelope.ConversationType.DIRECT.getValue())) {
                response.add(
                        new SignalMessageWSDto(
                            envelope.getId(),
                            envelope.getSender().getId(),
                            envelope.getOneToOneChat().getId(),
                            envelope.getConversationType(),
                            envelope.getCypherTextType(),
                            Base64.getEncoder().encodeToString(envelope.getCypherText()),
                            envelope.getCreatedAt()
                        ));
            }
        }
        return response;
    }

    @Transactional
    public void acknowledgeMessageDelivered(Long envelopeId, Long userId) {
        SignalEnvelope envelope = signalEnvelopeRepository.findById(envelopeId).orElse(null);
        if (envelope == null) {
            // Idempotente: otro ack en paralelo ya borró el sobre. No es error.
            return;
        }

        if (!envelope.getReceiver().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado para reconocer este mensaje");
        }

        try {
            signalEnvelopeRepository.delete(envelope);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            // Carrera: otra petición borró la fila entre el findById y el delete.
            logger.info("ack idempotente: envelope {} ya borrado por otra transacción", envelopeId);
        }
    }
}

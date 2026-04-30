package com.tfg.backend.GroupChat;

import com.tfg.backend.SignalEnvelope.SignalEnvelope;
import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.SignalEnvelope.dto.SignalDirectMessageWSDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageRequestDto;
import com.tfg.backend.SignalEnvelope.dto.SignalGroupMessageResponseDto;
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

}

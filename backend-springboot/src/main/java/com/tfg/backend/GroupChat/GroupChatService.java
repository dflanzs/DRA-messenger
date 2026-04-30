package com.tfg.backend.GroupChat;

import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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

package com.tfg.backend.OneToOneChat;

import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.User.UserService;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

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
}

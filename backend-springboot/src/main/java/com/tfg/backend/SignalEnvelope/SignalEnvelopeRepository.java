package com.tfg.backend.SignalEnvelope;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalEnvelopeRepository extends JpaRepository<SignalEnvelope, Long> {
    
    // TODO: query
    public List<SignalEnvelope> findByReceiver_IdAndStatus(Long userId, SignalEnvelope.MessageStatus status);

    List<SignalEnvelope> findByOneToOneChat_IdOrderByCreatedAtAsc(Long oneToOneChatId);

    List<SignalEnvelope> findByGroupChat_IdOrderByCreatedAtAsc(Long groupChatId);

    Optional<SignalEnvelope> findTopByOneToOneChat_IdOrderByCreatedAtDesc(Long oneToOneChatId);

    Optional<SignalEnvelope> findTopByGroupChat_IdOrderByCreatedAtDesc(Long groupChatId);
}

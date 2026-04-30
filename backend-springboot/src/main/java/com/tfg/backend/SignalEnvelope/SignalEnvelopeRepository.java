package com.tfg.backend.SignalEnvelope;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalEnvelopeRepository extends JpaRepository<SignalEnvelope, Long> {
    
    // TODO: query
    public List<SignalEnvelope> findByReceiver_IdAndStatus(Long userId, String status);
}

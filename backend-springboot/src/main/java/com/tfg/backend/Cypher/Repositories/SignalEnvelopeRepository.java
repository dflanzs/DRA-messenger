package com.tfg.backend.Cypher.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tfg.backend.Cypher.Entity.SignalEnvelope;

public interface SignalEnvelopeRepository extends JpaRepository<SignalEnvelope, Long> {
    
}

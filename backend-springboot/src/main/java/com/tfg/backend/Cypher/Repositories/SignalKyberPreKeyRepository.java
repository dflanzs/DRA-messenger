package com.tfg.backend.Cypher.Repositories;

import com.tfg.backend.Cypher.Entity.SignalKyberPreKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SignalKyberPreKeyRepository extends JpaRepository<SignalKyberPreKey, Long> {

    @Query("SELECT s FROM signal_kyber_pre_keys s WHERE s.user.id = :userId")
    SignalKyberPreKey getByUserId(Long userId);
}

package com.tfg.backend.Cypher.Repositories;

import com.tfg.backend.Cypher.Entity.SignalSignedPreKey;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SignalSignedPreKeyRepository extends JpaRepository<SignalSignedPreKey, Long> {

    @Query("SELECT s FROM SignalSignedPreKey s WHERE s.user.id = :userId")
    SignalSignedPreKey getByUserId(Long userId);

    @Query("SELECT s FROM SignalSignedPreKey s WHERE s.user.id = :userId AND s.preKeyId = :preKeyId")
    SignalSignedPreKey findByUserIdAndPreKeyId(Long userId, int preKeyId);
}

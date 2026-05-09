package com.tfg.backend.Cypher.Repositories;

import com.tfg.backend.Cypher.Entity.SignalOneTimePreKey;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SignalOneTimePreKeyRepository extends JpaRepository<SignalOneTimePreKey, Long> {

    @Query("SELECT s FROM SignalOneTimePreKey s WHERE s.user.id = :userId")
    List<SignalOneTimePreKey> getByUserId(Long userId);

    @Query("SELECT s FROM SignalOneTimePreKey s WHERE s.user.id = :userId AND s.consumedAt IS NULL ORDER BY s.uploadedAt ASC")
    SignalOneTimePreKey getUnconsumedPreKeyByUserId(Long userId);

    @Query("SELECT s FROM SignalOneTimePreKey s WHERE s.user.id = :userId AND s.preKeyId = :preKeyId")
    SignalOneTimePreKey findByUserIdAndPreKeyId(Long userId, int preKeyId);
}

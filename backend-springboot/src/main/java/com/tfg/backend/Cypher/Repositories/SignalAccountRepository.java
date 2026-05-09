package com.tfg.backend.Cypher.Repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import com.tfg.backend.Cypher.Entity.SignalAccount;

public interface SignalAccountRepository extends JpaRepository<SignalAccount, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SignalAccount s WHERE s.user.id = :userId")
    SignalAccount getByUserId(Long userId);
}

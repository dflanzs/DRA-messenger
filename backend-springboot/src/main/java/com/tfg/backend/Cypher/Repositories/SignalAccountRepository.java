package com.tfg.backend.Cypher.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.tfg.backend.Cypher.Entity.SignalAccount;

public interface SignalAccountRepository extends JpaRepository<SignalAccount, Long> {

    @Query("SELECT s FROM SignalAccount s WHERE s.user.id = :userId")
    SignalAccount getByUserId(Long userId);
}

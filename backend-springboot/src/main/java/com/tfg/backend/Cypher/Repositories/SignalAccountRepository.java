package com.tfg.backend.Cypher.Repositories;

import com.tfg.backend.Cypher.Entity.SignalAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SignalAccountRepository extends JpaRepository<SignalAccount, Long> {

    @Query("SELECT s FROM signal_accounts s WHERE s.user.id = :userId")
    SignalAccount getByUserId(Long userId);
}

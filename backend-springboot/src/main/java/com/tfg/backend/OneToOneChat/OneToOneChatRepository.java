package com.tfg.backend.OneToOneChat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OneToOneChatRepository extends JpaRepository<OneToOneChat, Long> {
    
    @Query("SELECT c FROM OneToOneChat c WHERE (c.user1.id = :userId1 AND c.user2.id = :userId2) OR (c.user1.id = :userId2 AND c.user2.id = :userId1)")
    Optional<OneToOneChat> findChatBetweenUsers(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
    
    @Query("SELECT c FROM OneToOneChat c WHERE c.user1.id = :userId OR c.user2.id = :userId")
    List<OneToOneChat> findChatsForUser(@Param("userId") Long userId);
}

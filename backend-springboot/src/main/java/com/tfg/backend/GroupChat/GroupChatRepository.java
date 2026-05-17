package com.tfg.backend.GroupChat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    
    boolean existsByIdAndUsers_Id(Long groupChatId, Long userId);

    Optional<GroupChat> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT DISTINCT g FROM GroupChat g JOIN g.users u WHERE g.deletedAt IS NULL AND u.id = :userId")
    List<GroupChat> findAllActiveByUserId(@Param("userId") Long userId);
}

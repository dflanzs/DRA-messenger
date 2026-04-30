package com.tfg.backend.GroupChat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    
    boolean existsByIdAndUsers_Id(Long groupChatId, Long userId);
}

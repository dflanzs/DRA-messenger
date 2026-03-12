package com.tfg.backend.Message;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    
    @Query("SELECT m FROM Message m WHERE m.oneToOneChat.id = :chatId ORDER BY m.createdAt ASC")
    List<Message> findByChatId(@Param("chatId") Long chatId);

    @Query("SELECT m FROM Message m WHERE m.oneToOneChat IN :chats AND m.sender.id != :userId AND m.read = false")
    List<Message> findUnreadMessagesInChats(@Param("chats") List<com.tfg.backend.OneToOneChat.OneToOneChat> chats, @Param("userId") Long userId);
}

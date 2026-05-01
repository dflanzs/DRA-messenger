package com.tfg.backend.GroupChat;

import com.tfg.backend.SignalEnvelope.SignalEnvelopeRepository;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;

import jakarta.transaction.Transactional;

import java.security.Principal;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class GroupChatService {

    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    public GroupChatService(
        GroupChatRepository groupChatRepository,
        UserRepository userRepository
    ) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public  ResponseEntity<String> addUserToGroupChat(Long userId, Long groupId, Principal principal) {
        GroupChat groupChat = groupChatRepository.findById(groupId).orElse(null);
        if (groupChat == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userRepository.findByEmail(principal.getName()).orElse(null);

        if (!groupChat.getUserIds().contains(currentUser.getId())) {
            return ResponseEntity.status(403).body("You are not a member of this group chat.");
        }

        User newUser = userRepository.findById(userId).orElse(null);
        if (newUser == null) {
            return ResponseEntity.notFound().build();
        }

        if (groupChat.getUserIds().stream().anyMatch(user -> newUser.getId().equals(userId))) {
            return ResponseEntity.badRequest().body("User is already a member of the group chat.");
        }

        groupChat.getUserIds().add(userId);
        groupChatRepository.save(groupChat);

        return ResponseEntity.ok("User added to group chat successfully.");
    }

    @Transactional
    public  ResponseEntity<String> removeUserFromGroupChat(Long userId, Long groupId, Principal principal) {
        GroupChat groupChat = groupChatRepository.findById(groupId).orElse(null);
        if (groupChat == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
        if (!groupChat.getUserIds().contains(currentUser.getId())) {
            return ResponseEntity.status(403).body("You are not a member of this group chat.");
        }

        User newUser = userRepository.findById(userId).orElse(null);
        if (newUser == null) {
            return ResponseEntity.notFound().build();
        }

        if (!groupChat.getUserIds().stream().anyMatch(user -> newUser.getId().equals(userId))) {
            return ResponseEntity.badRequest().body("User is not a member of the group chat.");
        }

        groupChat.getUserIds().remove(userId);
        groupChatRepository.save(groupChat);

        return ResponseEntity.ok("User removed from group chat successfully.");
    }

    @Transactional
    public ResponseEntity<Set<Long>> getUsersInGroupChat(Long groupId, Principal principal){
        GroupChat groupChat = groupChatRepository.findById(groupId).orElse(null);
        if (groupChat == null) {
            return ResponseEntity.notFound().build();
        }

        User currentUser = userRepository.findByEmail(principal.getName()).orElse(null);
        if (!groupChat.getUserIds().contains(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        Set<Long> userIds = groupChat.getUserIds();
        return ResponseEntity.ok(userIds);
    }
}

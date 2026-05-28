package com.tfg.backend.GroupChat;

import com.tfg.backend.Audit.AuditService;
import com.tfg.backend.Chat.dto.CreateGroupChatRequestDto;
import com.tfg.backend.Enums.AuditAction;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import com.tfg.backend.User.UserService;

import jakarta.transaction.Transactional;

import java.security.Principal;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GroupChatService {

    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditService auditService;

    public GroupChatService(
        GroupChatRepository groupChatRepository,
        UserRepository userRepository,
        UserService userService,
        AuditService auditService
    ) {
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public GroupChat create(CreateGroupChatRequestDto request, Principal principal) {
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de grupo inválido");
        }

        Long currentUserId = userService.getByEmail(principal.getName()).getId();
        GroupChat groupChat = new GroupChat();
        groupChat.setName(request.name());
        groupChat.addMember(userService.getById(currentUserId));
        if (request.userIds() != null) {
            request.userIds().stream()
                .filter(userId -> !currentUserId.equals(userId))
                .map(userService::getById)
                .forEach(groupChat::addMember);
        }
        if (groupChat.getUserIds().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grupo necesita al menos 2 miembros");
        }

        GroupChat saved = groupChatRepository.save(groupChat);
        auditService.record(AuditAction.CREATE_GROUP_CHAT, currentUserId);
        return saved;
    }

    @Transactional
    public void delete(Long id, Principal principal) {
        if (!groupChatRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Grupo no encontrado");
        }

        Set<Long> memberIds = groupChatRepository.findById(id).map(GroupChat::getUserIds).orElse(Set.of());
        Long currentUserId = userService.getByEmail(principal.getName()).getId();
        if (!memberIds.contains(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres miembro de este grupo");
        }
        groupChatRepository.deleteById(id);
        auditService.record(AuditAction.DELETE_GROUP_CHAT, currentUserId);
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
        groupChat.setUpdatedAt(java.time.LocalDateTime.now());
        groupChatRepository.save(groupChat);
        auditService.record(AuditAction.ADD_USER_TO_GROUP_CHAT, currentUser.getId());

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
        groupChat.setUpdatedAt(java.time.LocalDateTime.now());
        groupChatRepository.save(groupChat);
        auditService.record(AuditAction.REMOVE_USER_FROM_GROUP_CHAT, currentUser.getId());

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

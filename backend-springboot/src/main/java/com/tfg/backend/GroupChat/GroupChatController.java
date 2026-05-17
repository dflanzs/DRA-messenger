package com.tfg.backend.GroupChat;

import com.tfg.backend.Chat.dto.CreateGroupChatRequestDto;
import com.tfg.backend.Chat.dto.GroupChatSummaryDto;
import com.tfg.backend.User.UserService;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@RestController
@RequestMapping("/api/group-chats")
public class GroupChatController {

    private final GroupChatRepository groupChatRepository;
    private final GroupChatService groupcChatService;
    private final UserService userService;

    public GroupChatController(
            GroupChatService groupChatService,
            GroupChatRepository groupChatRepository,
            UserService userService
    ) {
        this.groupcChatService = groupChatService;
        this.groupChatRepository = groupChatRepository;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    public List<GroupChat> list() {
        return groupChatRepository.findAll();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public List<GroupChatSummaryDto> listForCurrentUser(Principal principal) {
        Long currentUserId = userService.getByEmail(principal.getName()).getId();
        return groupChatRepository.findAllActiveByUserId(currentUserId).stream()
            .map(groupChat -> new GroupChatSummaryDto(
                groupChat.getId(),
                groupChat.getName(),
                groupChat.getUserIds(),
                groupChat.getCreatedAt()
            ))
            .toList();
    }

    @GetMapping("/{groupId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupChatSummaryDto> getById(@PathVariable Long groupId, Principal principal) {
        Long currentUserId = userService.getByEmail(principal.getName()).getId();
        GroupChat groupChat = groupChatRepository.findByIdAndDeletedAtIsNull(groupId).orElse(null);
        if (groupChat == null || !groupChat.getUserIds().contains(currentUserId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new GroupChatSummaryDto(
            groupChat.getId(),
            groupChat.getName(),
            groupChat.getUserIds(),
            groupChat.getCreatedAt()
        ));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GroupChatSummaryDto> create(@RequestBody CreateGroupChatRequestDto request, Principal principal) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
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
            return ResponseEntity.badRequest().build();
        }
        GroupChat saved = groupChatRepository.save(groupChat);
        return ResponseEntity.ok(new GroupChatSummaryDto(
            saved.getId(),
            saved.getName(),
            saved.getUserIds(),
            saved.getCreatedAt()
        ));
    }

    @ResponseBody
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!groupChatRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        groupChatRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/add/user/{userId}/group/{groupId}")    
    public ResponseEntity<String> addUserToGroupChat(
            @PathVariable Long userId,
            @PathVariable Long groupId,
            Principal principal
    ) {
        ResponseEntity<String> response = groupcChatService.addUserToGroupChat(userId, groupId, principal);
        return response;
    }

    @ResponseBody
    @GetMapping("/remove/user/{userId}/group/{groupId}")
    public ResponseEntity<String> removeUserFromGroupChat(
            @PathVariable Long userId,
            @PathVariable Long groupId,
            Principal principal
    ) {
        ResponseEntity<String> response = groupcChatService.removeUserFromGroupChat(userId, groupId, principal);
        return response;
    }

    @GetMapping("/{groupId}/users")
    public ResponseEntity<Set<Long>> getUsersInGroupChat(
            @PathVariable Long groupId,
            Principal principal
    ) {
        ResponseEntity<Set<Long>> response = groupcChatService.getUsersInGroupChat(groupId, principal);
        return response;
    }
}

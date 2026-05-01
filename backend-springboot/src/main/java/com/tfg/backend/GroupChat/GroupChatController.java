package com.tfg.backend.GroupChat;

import com.tfg.backend.User.UserService;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/group-chats")
public class GroupChatController {

    private final GroupChatRepository groupChatRepository;
    private final GroupChatService groupcChatService;

    public GroupChatController(
            GroupChatService groupChatService,
            GroupChatRepository groupChatRepository
    ) {
        this.groupcChatService = groupChatService;
        this.groupChatRepository = groupChatRepository;
    }

    @GetMapping
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    public List<GroupChat> list() {
        return groupChatRepository.findAll();
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

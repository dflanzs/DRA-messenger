package com.tfg.backend.User;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.backend.Chat.dto.ChatUserDto;
import com.tfg.backend.User.dto.UpdateUserDto;
import com.tfg.backend.User.dto.UpdateUserRoleDto;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public List<User> list() {
        return userService.list();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/active")
    public List<ChatUserDto> listActive() {
        return userService.list().stream()
            .map(user -> new ChatUserDto(user.getId(), user.getName(), user.getEmail(), user.isOnlineStatus()))
            .toList();
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #id)")
    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateUserDto updateUserDto) {
        return ResponseEntity.ok(userService.update(id, updateUserDto));
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.isSelf(authentication, #id)")
    @PutMapping("/{id}/online-status")
    public ResponseEntity<User> setOnlineStatus(@PathVariable Long id,
                                                @RequestBody boolean onlineStatus) {
        return ResponseEntity.ok(userService.setOnlineStatus(id, onlineStatus));
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateRole(@PathVariable Long id,
                                           @RequestBody UpdateUserRoleDto updateUserRoleDto) {
        return ResponseEntity.ok(userService.updateRole(id, updateUserRoleDto.getRole()));
    }
}

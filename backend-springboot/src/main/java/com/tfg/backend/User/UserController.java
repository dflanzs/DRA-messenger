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
import com.tfg.backend.User.dto.UserResponseDto;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public List<UserResponseDto> list() {
        return userService.list().stream()
            .map(UserResponseDto::fromUser)
            .toList();
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
    public ResponseEntity<UserResponseDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponseDto.fromUser(userService.getById(id)));
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #id)")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateUserDto updateUserDto) {
        return ResponseEntity.ok(UserResponseDto.fromUser(userService.update(id, updateUserDto)));
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.isSelf(authentication, #id)")
    @PutMapping("/{id}/online-status")
    public ResponseEntity<UserResponseDto> setOnlineStatus(@PathVariable Long id,
                                                           @RequestBody boolean onlineStatus) {
        return ResponseEntity.ok(UserResponseDto.fromUser(userService.setOnlineStatus(id, onlineStatus)));
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponseDto> updateRole(@PathVariable Long id,
                                                      @RequestBody UpdateUserRoleDto updateUserRoleDto) {
        return ResponseEntity.ok(UserResponseDto.fromUser(userService.updateRole(id, updateUserRoleDto.getRole())));
    }
}

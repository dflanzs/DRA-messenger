package com.tfg.backend.User;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.backend.User.dto.UpdateUserDto;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateUserDto updateUserDto,
                                       Principal principal) {
        requireOwnership(principal, id);
        return ResponseEntity.ok(userService.update(id, updateUserDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        requireOwnership(principal, id);
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/online-status")
    public ResponseEntity<User> setOnlineStatus(@PathVariable Long id,
                                                @RequestBody boolean onlineStatus,
                                                Principal principal) {
        requireOwnership(principal, id);
        return ResponseEntity.ok(userService.setOnlineStatus(id, onlineStatus));
    }

    private void requireOwnership(Principal principal, Long userId) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        User current = userService.getByEmail(principal.getName());
        if (!current.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No tienes permiso para modificar este usuario");
        }
    }
}

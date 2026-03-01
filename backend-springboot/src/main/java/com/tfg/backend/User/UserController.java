package com.tfg.backend.User;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tfg.backend.User.dto.CreateUserDto;
import com.tfg.backend.User.dto.UpdateUserDto;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<User> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> user = repository.findById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> create(@Valid @RequestBody CreateUserDto createUserDto) {
        if (!UserService.validatePassword(createUserDto.getPassword())) {
            return ResponseEntity.badRequest().build();
        }

        User user = new User();
        user.setName(createUserDto.getName());
        user.setEmail(createUserDto.getEmail());
        user.setPassword(createUserDto.getPassword());
        user.setOnlineStatus(false);
        User newUser = repository.save(user);

        URI location = URI.create("/api/users/" + newUser.getId());

        return ResponseEntity.created(location).body(newUser);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @Valid @RequestBody UpdateUserDto updateUserDto) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        User user = new User();
        switch (updateUserDto.getMode()) {
            case UpdateUserDto.MODE_NAME:
                user.setName(updateUserDto.getUpdatedValue());
                break;
            case UpdateUserDto.MODE_EMAIL:
                user.setEmail(updateUserDto.getUpdatedValue());
                break;
            case UpdateUserDto.MODE_PASSWORD:
                    if (!UserService.validatePassword(updateUserDto.getUpdatedValue())) {
                        return ResponseEntity.badRequest().build();
                    }
                    user.setPassword(updateUserDto.getUpdatedValue());
                break;
            default:
                throw new IllegalArgumentException("Invalid mode: " + updateUserDto.getMode());
        }

        user.setId(id);
        return ResponseEntity.ok(repository.save(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/online-status")
    public ResponseEntity<User> setOnlineStatus(@PathVariable Long id, @RequestBody boolean onlineStatus) {
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<User> optionalUser = repository.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = optionalUser.get();
        user.setOnlineStatus(onlineStatus);
        User updatedUser = repository.save(user);
        
        return ResponseEntity.ok(updatedUser);
    }

    
}

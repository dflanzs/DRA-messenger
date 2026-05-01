package com.tfg.backend.GroupChat;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "group_chats")
public class GroupChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    private Set<User> users = new HashSet<>();

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime deletedAt;

    public Long getId() {
        return id;
    }

      public Set<Long> getUserIds() {
        return users.stream()
                .map(User::getId)
                .collect(Collectors.toSet());
    }
}

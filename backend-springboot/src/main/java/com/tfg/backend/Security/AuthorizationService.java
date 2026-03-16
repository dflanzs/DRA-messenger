package com.tfg.backend.Security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.tfg.backend.Enums.UserRole;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;

@Service
public class AuthorizationService {
    private final UserService userService;

    public AuthorizationService(UserService userService){
        this.userService = userService;
    }

    public boolean isAdmin(Authentication authentication){
        if (authentication == null || authentication.getName() == null) {
            return false;
        }

        User currentUser = userService.getByEmail(authentication.getName());

        return currentUser.getRole().name().equals(UserRole.ADMIN.name());
    }

    public boolean isSelf(Authentication authentication, Long userId) {
        if (authentication == null || authentication.getName() == null) {
            return false;
        }

        User currentUser = userService.getByEmail(authentication.getName());

        return currentUser.getId().equals(userId);
    }
    public boolean isSelfOrAdmin(Authentication authentication, Long userId) {
        return isAdmin(authentication) || isSelf(authentication, userId);
    }
}

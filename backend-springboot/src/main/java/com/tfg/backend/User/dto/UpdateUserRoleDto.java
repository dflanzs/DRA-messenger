package com.tfg.backend.User.dto;

import com.tfg.backend.Enums.UserRole;

public class UpdateUserRoleDto {
    private UserRole role;

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}

package com.tfg.backend.User.dto;

import jakarta.validation.constraints.Email;

public class CreateUserDto {

        
    private String username;

    @Email
    private String email;

    
    private String password;

    public CreateUserDto() {
    }

    public CreateUserDto(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getName() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

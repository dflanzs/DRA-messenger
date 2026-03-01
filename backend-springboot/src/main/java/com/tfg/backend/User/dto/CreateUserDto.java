package com.tfg.backend.User.dto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private boolean validatePassword(String password) {
        Pattern regex = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
        Matcher matcher = regex.matcher(password);

        if (!matcher.matches()){
            return false;
        }

        return true;
    }

    public void setPassword(String password) {
        // Validar la contraseña (por ejemplo, longitud mínima, complejidad, etc.)
        if (!validatePassword(password)) {
            throw new IllegalArgumentException(
                "La contraseña debe tener al menos 8 caracteres, incluir al menos una letra mayúscula, una letra minúscula, un número y un carácter especial.");
        }

        this.password = password;
    }
}

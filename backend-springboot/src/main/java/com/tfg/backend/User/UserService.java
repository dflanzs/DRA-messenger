package com.tfg.backend.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    public boolean validatePassword(String password) {
        Pattern regex = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
        Matcher matcher = regex.matcher(password);

        if (!matcher.matches()){
            return false;
        }

        return true;
    }

    public boolean verifyPassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)){
            return false;
        }

        return true;
    }
}

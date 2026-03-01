package com.tfg.backend.User;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserService {
    public static boolean validatePassword(String password) {
        Pattern regex = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$");
        Matcher matcher = regex.matcher(password);

        if (!matcher.matches()){
            return false;
        }

        return true;
    }

    public static boolean verifyPassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)){
            return false;
        }

        return true;
    }
}

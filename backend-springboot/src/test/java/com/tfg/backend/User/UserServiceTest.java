package com.tfg.backend.User;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    private final UserService userService = new UserService();

    @Test
    void validatePassword_returnsTrue_whenPasswordMeetsPolicy() {
        String validPassword = "StrongP@ss1";

        boolean result = userService.validatePassword(validPassword);

        assertTrue(result);
    }

    @Test
    void validatePassword_returnsFalse_whenPasswordMissesUppercase() {
        String invalidPassword = "weakp@ss1";

        boolean result = userService.validatePassword(invalidPassword);

        assertFalse(result);
    }

    @Test
    void validatePassword_returnsFalse_whenPasswordMissesSpecialChar() {
        String invalidPassword = "Weakpass1";

        boolean result = userService.validatePassword(invalidPassword);

        assertFalse(result);
    }

    @Test
    void verifyPassword_returnsTrue_whenBothPasswordsMatch() {
        boolean result = userService.verifyPassword("StrongP@ss1", "StrongP@ss1");

        assertTrue(result);
    }

    @Test
    void verifyPassword_returnsFalse_whenPasswordsDoNotMatch() {
        boolean result = userService.verifyPassword("StrongP@ss1", "StrongP@ss2");

        assertFalse(result);
    }
}

package com.tfg.backend.User.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDtoTest {

    // ===== CreateUserDto Tests =====
    @Test
    void createUserDto_constructorWithArgs_setsValuesCorrectly() {
        CreateUserDto dto = new CreateUserDto("testuser", "test@example.com", "StrongP@ss1");

        assertEquals("testuser", dto.getName());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("StrongP@ss1", dto.getPassword());
    }

    @Test
    void createUserDto_defaultConstructor_createsInstance() {
        CreateUserDto dto = new CreateUserDto();

        assertNotNull(dto);
        assertNull(dto.getName());
        assertNull(dto.getEmail());
        assertNull(dto.getPassword());
    }

    // ===== UpdateUserDto Tests =====
    @Test
    void updateUserDto_withModeNameIsValid_createsInstance() {
        UpdateUserDto dto = new UpdateUserDto("NewName", "name");

        assertEquals("NewName", dto.getUpdatedValue());
        assertEquals("name", dto.getMode());
    }

    @Test
    void updateUserDto_withModeEmailIsValid_createsInstance() {
        UpdateUserDto dto = new UpdateUserDto("newemail@example.com", "email");

        assertEquals("newemail@example.com", dto.getUpdatedValue());
        assertEquals("email", dto.getMode());
    }

    @Test
    void updateUserDto_withModePasswordIsValid_createsInstance() {
        UpdateUserDto dto = new UpdateUserDto("NewPass@123", "password");

        assertEquals("NewPass@123", dto.getUpdatedValue());
        assertEquals("password", dto.getMode());
    }

    @Test
    void updateUserDto_withInvalidMode_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new UpdateUserDto("someValue", "invalidMode");
        });
    }

    @Test
    void updateUserDto_withNullMode_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> {
            new UpdateUserDto("someValue", null);
        });
    }

    @Test
    void updateUserDto_modeConstants_haveCorrectValues() {
        assertEquals("name", UpdateUserDto.MODE_NAME);
        assertEquals("email", UpdateUserDto.MODE_EMAIL);
        assertEquals("password", UpdateUserDto.MODE_PASSWORD);
    }

    // ===== UpdateOnlineStatusDto Tests =====
    @Test
    void updateOnlineStatusDto_defaultConstructor_createsInstance() {
        UpdateOnlineStatusDto dto = new UpdateOnlineStatusDto();

        assertNotNull(dto);
        assertFalse(dto.isOnline());
    }

    @Test
    void updateOnlineStatusDto_constructorWithTrue_setsOnlineToTrue() {
        UpdateOnlineStatusDto dto = new UpdateOnlineStatusDto(true);

        assertTrue(dto.isOnline());
    }

    @Test
    void updateOnlineStatusDto_constructorWithFalse_setsOnlineToFalse() {
        UpdateOnlineStatusDto dto = new UpdateOnlineStatusDto(false);

        assertFalse(dto.isOnline());
    }

    @Test
    void updateOnlineStatusDto_setOnline_updatesValue() {
        UpdateOnlineStatusDto dto = new UpdateOnlineStatusDto(false);

        dto.setOnline(true);
        assertTrue(dto.isOnline());

        dto.setOnline(false);
        assertFalse(dto.isOnline());
    }
}

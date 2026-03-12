package com.tfg.backend.User;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.tfg.backend.Security.CustomUserDetailsService;
import com.tfg.backend.Security.JwtAuthenticationFilter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
        private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void list_returnsUsers_whenUsersExist() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("User");
        user.setEmail("user@example.com");

        when(userService.list()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("User"));
    }

    @Test
    void get_returnsNotFound_whenUserDoesNotExist() throws Exception {
                when(userService.getById(99L)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated_whenPasswordIsValid() throws Exception {
        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setName("User");
        savedUser.setEmail("user@example.com");
        savedUser.setPassword("StrongP@ss1");

        when(userService.create(any())).thenReturn(savedUser);

        String body = """
                {
                  "username": "User",
                  "email": "user@example.com",
                  "password": "StrongP@ss1"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/10"))
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.name").value("User"));
    }

    @Test
    void create_returnsBadRequest_whenPasswordIsInvalid() throws Exception {
                when(userService.create(any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        String body = """
                {
                  "username": "User",
                  "email": "user@example.com",
                  "password": "weak"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returnsNoContent_whenUserExists() throws Exception {
        User user = new User();
        user.setId(3L);

        org.mockito.Mockito.doNothing().when(userService).delete(3L);

        mockMvc.perform(delete("/api/users/3"))
                .andExpect(status().isNoContent());
    }

    @Test
    void setOnlineStatus_returnsOk_whenUserExists() throws Exception {
        User user = new User();
        user.setId(4L);
        user.setName("User");
        user.setEmail("user@example.com");
        user.setOnlineStatus(false);

        User savedUser = new User();
        savedUser.setId(4L);
        savedUser.setName("User");
        savedUser.setEmail("user@example.com");
        savedUser.setOnlineStatus(true);
        savedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.setOnlineStatus(4L, true)).thenReturn(savedUser);

        mockMvc.perform(put("/api/users/4/online-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlineStatus").value(true));
    }

    @Test
    void update_returnsOk_whenUpdatingName() throws Exception {
        User user = new User();
        user.setId(5L);
        user.setName("OldName");
        user.setEmail("user@example.com");

        User updatedUser = new User();
        updatedUser.setId(5L);
        updatedUser.setName("NewName");
        updatedUser.setEmail("user@example.com");
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.update(any(), any())).thenReturn(updatedUser);

        String body = """
                {
                  "updatedValue": "NewName",
                  "mode": "name"
                }
                """;

        mockMvc.perform(put("/api/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"));
    }

    @Test
    void update_returnsOk_whenUpdatingEmail() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(6L);
        updatedUser.setName("User");
        updatedUser.setEmail("newemail@example.com");
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.update(any(), any())).thenReturn(updatedUser);

        String body = """
                {
                  "updatedValue": "newemail@example.com",
                  "mode": "email"
                }
                """;

        mockMvc.perform(put("/api/users/6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("newemail@example.com"));
    }

    @Test
    void update_returnsOk_whenUpdatingPassword() throws Exception {
        User updatedUser = new User();
        updatedUser.setId(7L);
        updatedUser.setName("User");
        updatedUser.setEmail("user@example.com");
        updatedUser.setPassword("NewStrongP@ss1");
        updatedUser.setUpdatedAt(LocalDateTime.now());

        when(userService.update(any(), any())).thenReturn(updatedUser);

        String body = """
                {
                  "updatedValue": "NewStrongP@ss1",
                  "mode": "password"
                }
                """;

        mockMvc.perform(put("/api/users/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void update_returnsBadRequest_whenPasswordIsInvalid() throws Exception {
                when(userService.update(any(), any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST));

        String body = """
                {
                  "updatedValue": "weak",
                  "mode": "password"
                }
                """;

        mockMvc.perform(put("/api/users/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsNotFound_whenUserDoesNotExist() throws Exception {
                when(userService.update(any(), any())).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        String body = """
                {
                  "updatedValue": "NewName",
                  "mode": "name"
                }
                """;

        mockMvc.perform(put("/api/users/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNotFound_whenUserDoesNotExist() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND))
                .when(userService).delete(99L);

        mockMvc.perform(delete("/api/users/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void setOnlineStatus_returnsNotFound_whenUserDoesNotExist() throws Exception {
                when(userService.setOnlineStatus(99L, true)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(put("/api/users/99/online-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isNotFound());
    }
}

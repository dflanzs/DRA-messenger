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
import java.util.Optional;

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
    private UserRepository userRepository;

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

        when(userRepository.findAllByDeletedAtIsNull()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("User"));
    }

    @Test
    void get_returnsNotFound_whenUserDoesNotExist() throws Exception {
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

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

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

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

        when(userRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

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

        when(userRepository.findByIdAndDeletedAtIsNull(4L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        mockMvc.perform(put("/api/users/4/online-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlineStatus").value(true));
    }
}

package com.tfg.backend.User;

import com.tfg.backend.Chat.dto.ChatUserDto;
import com.tfg.backend.Enums.UserRole;
import com.tfg.backend.User.dto.CreateUserDto;
import com.tfg.backend.User.dto.UpdateUserDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAllByDeletedAtIsNull();
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        return userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Transactional
    public User create(CreateUserDto createUserDto) {
        if (!validatePassword(createUserDto.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña inválida");
        }

        User user = new User();
        user.setName(createUserDto.getName());
        user.setEmail(createUserDto.getEmail());
        user.setPassword(passwordEncoder.encode(createUserDto.getPassword()));
        user.setOnlineStatus(false);

        return userRepository.save(user);
    }

    @Transactional
    public User update(Long id, UpdateUserDto updateUserDto) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        if (!userRepository.existsByIdAndDeletedAtIsNull(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }

        User user = new User();
        switch (updateUserDto.getMode()) {
            case UpdateUserDto.MODE_NAME:
                user.setName(updateUserDto.getUpdatedValue());
                break;
            case UpdateUserDto.MODE_EMAIL:
                user.setEmail(updateUserDto.getUpdatedValue());
                break;
            case UpdateUserDto.MODE_PASSWORD:
                if (!validatePassword(updateUserDto.getUpdatedValue())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contraseña inválida");
                }
                user.setPassword(passwordEncoder.encode(updateUserDto.getUpdatedValue()));
                break;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modo de actualización inválido");
        }

        user.setId(id);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User setOnlineStatus(Long id, boolean onlineStatus) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setOnlineStatus(onlineStatus);
        return userRepository.save(user);
    }

    @Transactional
    public User setOnlineStatusByEmail(String email, boolean onlineStatus) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email inválido");
        }

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setOnlineStatus(onlineStatus);
        return userRepository.save(user);
    }

    @Transactional
    public User updateRole(Long id, UserRole role) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id inválido");
        }

        if (role == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        user.setRole(role);
        return userRepository.save(user);
    }

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

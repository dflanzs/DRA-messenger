package com.tfg.backend.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    int countByOnlineStatusTrue();
    // Métodos para soft delete (excluyen usuarios eliminados)
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    List<User> findAllByDeletedAtIsNull();
    boolean existsByIdAndDeletedAtIsNull(Long id);
    int countByDeletedAtIsNull();
}

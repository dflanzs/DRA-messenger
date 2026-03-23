package com.tfg.backend.User;

import com.tfg.backend.Enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    // Métodos para soft delete (excluyen usuarios eliminados)
    Optional<User> findByEmailAndDeletedAtIsNull(String email);
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    List<User> findAllByDeletedAtIsNull();
    List<User> findAllByDeletedAtIsNullAndRole(UserRole role);
    boolean existsByIdAndDeletedAtIsNull(Long id);
}

package com.tfg.backend.Audit;

import com.tfg.backend.Enums.AuditAction;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditService {
    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional
    public Audit record(AuditAction action, Long userId) {
        if (action == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Acción inválida");
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id de usuario inválido");
        }

        Audit audit = new Audit(action, userId, System.currentTimeMillis());
        return auditRepository.save(audit);
    }

    @Transactional(readOnly = true)
    public List<Audit> list() {
        return list(null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<Audit> list(Long userId, AuditAction action, Long from, Long to) {
        Specification<Audit> spec = (root, query, cb) -> {
            // SQL query
            List<Predicate> predicates = new ArrayList<>();

            // Add conditions based on provided parameters
            if (userId != null) {
                // WHERE user_id = :userId
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (action != null) {
                // WHERE action = :action
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (from != null) {
                // WHERE timestamp >= :from
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                // WHERE timestamp <= :to
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "timestamp"));
    }
}

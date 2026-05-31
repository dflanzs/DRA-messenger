package com.tfg.backend.Audit;

import com.tfg.backend.Enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit")
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", updatable = false)
    private AuditAction action;

    @Column(name = "userId", updatable = false)
    private Long userId;

    @Column(name = "timestamp", updatable = false)
    private Long timestamp;

    public Audit() {}

    public Audit(AuditAction action, Long userId, Long timestamp) {
        this.action = action;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTimestamp() {
        return timestamp;
    }
}

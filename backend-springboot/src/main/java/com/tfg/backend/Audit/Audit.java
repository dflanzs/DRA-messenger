package com.tfg.backend.Audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit")
public class Audit {
    
    private enum Action {
        CREATE_USER, UPDATE_USER, DELETE_USER, LOGIN, LOGOUT, 
        CREATE_OTO_CHAT, UPDATE_OTO_CHAT, DELETE_OTO_CHAT,
        CREATE_GROUP_CHAT, UPDATE_GROUP_CHAT, DELETE_GROUP_CHAT,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action")
    private Action action;

    @Column(name = "userId")
    private Long userId;

    @Column(name = "timestamp")
    private Long timestamp;

    public Audit() {}

    public Audit(Action action, Long userId, Long timestamp) {
        this.action = action;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}   

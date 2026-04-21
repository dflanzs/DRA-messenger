package com.tfg.backend.Cypher.Entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Check;
import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Represents a user's account on the Signal protocol, storing necessary cryptographic material and identifiers.
 * Each user has a single SignalAccount, which is used to manage their identity and pre-keys for secure communication.
 */

@Entity
@Table(name = "signal_accounts")
@Check(constraints = "device_id = 1") // Enforce that device_id is always 1
public class SignalAccount {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // deviceId is hardcoded to 1 because libsignal requires this filed but multiple devices per user are not supported in this implementation 
    @Column(name = "device_id", nullable = false)
    private int deviceId = 1;

    // To build sessions
    @Column(name = "registration_id", nullable = false)
    private int registrationId;

    // Public key of the user on Signal
    @Lob
    @Column(name = "identity_key_public", nullable = false)
    private byte[] identityKeyPublic;

    // Active pre-key for the user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_signed_pre_key_id", nullable = false)
    private SignalSignedPreKey activeSignedPreKey;

    //Active kyber pre-key for the user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_kyber_pre_key_id", nullable = false)
    private SignalKyberPreKey activeKyberPreKey;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SignalAccount() {
        // Default constructor for JPA
    }

    public SignalAccount(SignalSignedPreKey activeSignedPreKey, SignalKyberPreKey activeKyberPreKey, User user, int registrationId, byte[] identityKeyPublic) {
        this.activeSignedPreKey = activeSignedPreKey;
        this.activeKyberPreKey = activeKyberPreKey;
        this.user = user;
        this.registrationId = registrationId;
        this.identityKeyPublic = identityKeyPublic;
    }

    @PrePersist
    protected void onCreate() {
        if (this.deviceId != 1) {
            this.deviceId = 1;
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User GetUser() {
        return this.user;
    }

    public void SetUser(User user) {
        this.user = user;
    }

    public int GetDeviceId() {
        return this.deviceId;
    }

     public int GetRegistrationId() {
        return this.registrationId;
    }

    public void SetRegistrationId(int registrationId) {
        this.registrationId = registrationId;
    }

    public byte[] GetActiveIdentityKeyPublic () {
        return this.identityKeyPublic;
    }

    public void SetActiveIdentityKeyPublic (byte[] identityKeyPublic) {
        this.identityKeyPublic = identityKeyPublic;
    }

    public SignalSignedPreKey GetActiveSignedPreKey () {
        return this.activeSignedPreKey;
    }

    public void SetActiveSignedPreKey (SignalSignedPreKey activeSignedPreKey) {
        this.activeSignedPreKey = activeSignedPreKey;
    }

    public SignalKyberPreKey GetActiveKyberPreKey () {
        return this.activeKyberPreKey;
    }

    public void SetActiveKyberPreKey (SignalKyberPreKey activeKyberPreKey) {
        this.activeKyberPreKey = activeKyberPreKey;
    }

     public LocalDateTime GetCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime GetUpdatedAt() {
        return this.updatedAt;
    }
}

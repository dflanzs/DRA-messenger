package com.tfg.backend.Cypher.Entity;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "signal_accounts")
public class SignalAccount {
    @Id
    private Long id;

    // To identify the user associated with the cryptographic material
    @OneToOne
    private User user;

    // deviceId is hardcoded to 1 because libsignal requires this filed but multiple devices per user are not supported in this implementation 
    private int deviceId = 1;

    // To build sessions
    private int registrationId;

    // Public key of the user on Signal
    private byte[] identityKeyPublic;

    // Active pre-key for the user
    private SignalSignedPreKeys activeSignedPreKey;

    //Active kyber pre-key for the user
    private SignalKyberPreKey activeKyberPreKey;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public SignalAccount(SignalSignedPreKeys activeSignedPreKey, SignalKyberPreKey activeKyberPreKey, User user, int registrationId, byte[] identityKeyPublic) {
        this.activeSignedPreKey = activeSignedPreKey;
        this.activeKyberPreKey = activeKyberPreKey; 
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

    public SignalSignedPreKeys GetActiveSignedPreKey () {
        return this.activeSignedPreKey;
    }

    public void SetActiveSignedPreKey (SignalSignedPreKeys activeSignedPreKey) {
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
}

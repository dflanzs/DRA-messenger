package com.tfg.backend.Cypher.Entity;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
@Table(name = "signal_signed_pre_keys")
public class SignalSignedPreKeys {
    
    @Id
    private int Id;

    @OneToOne
    private User user;
    
    // Logic id on Signal protocol, not the database id
    private int preKeyId;

    // Public key
    private byte[] publicKey;

    // Signature of the pre-key, signed by the user's identity key
    private byte[] signature;

    private boolean isActive;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime retiredAt;

    public SignalSignedPreKeys(int preKeyId, byte[] publicKey, byte[] signature, User user) {
        this.preKeyId = preKeyId;
        this.publicKey = publicKey;
        this.signature = signature;
        this.user = user;
        this.isActive = true;
        this.uploadedAt = LocalDateTime.now();
    }

    public User GetUser() {
        return this.user;
    }

    public void SetUser(User user) {
        this.user = user;
    }

    public int GetPreKeyId() {
        return this.preKeyId;
    }

    public byte[] GetPublicKey() {
        return this.publicKey;
    }

    public byte[] GetSignature() {
        return this.signature;
    }

    public boolean IsActive() {
        return this.isActive;
    }

    public void Retire() {
        this.isActive = false;
        this.retiredAt = LocalDateTime.now();
    }
}


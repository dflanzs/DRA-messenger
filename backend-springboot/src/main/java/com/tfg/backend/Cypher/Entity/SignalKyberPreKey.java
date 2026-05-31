package com.tfg.backend.Cypher.Entity;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.User.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/*
 * Save user's signed pre-keys history and mark which one is active. 
 */

@Entity
@Table(
    name = "signal_kyber_pre_keys",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_signal_kyber_pre_keys_user_prekey", columnNames = {"user_id", "kyber_pre_key_id"})
    }
)
public class SignalKyberPreKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Logic id on Signal protocol, not the database id
    @Column(name = "kyber_pre_key_id", nullable = false)
    private int preKeyId;

    // Public key 
    @Lob
    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    // Signature of the pre-key, signed by the user's identity key
    @Lob
    @Column(name = "signature", nullable = false)
    private byte[] signature;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "retired_at")
    private LocalDateTime retiredAt;

    public SignalKyberPreKey() {
        // Default constructor for JPA
    }

    public SignalKyberPreKey(int preKeyId, byte[] publicKey, byte[] signature, User user) {
        this.preKeyId = preKeyId;
        this.publicKey = publicKey;
        this.signature = signature;
        this.user = user;
        this.isActive = true;
        this.uploadedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public User GetUser() {
        return this.user;
    }

    public Long GetId() {
        return this.id;
    }

    public void SetUser(User user) {
        this.user = user;
    }

    public int GetPreKeyId() {
        return this.preKeyId;
    }

    public void SetPreKeyId(int preKeyId) {
        this.preKeyId = preKeyId;
    }

    public byte[] GetPublicKey() {
        return this.publicKey;
    }

    public void SetPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] GetSignature() {
        return this.signature;
    }

    public void SetSignature(byte[] signature) {
        this.signature = signature;
    }

    public boolean IsActive() {
        return this.isActive;
    }

    public void Retire() {
        this.isActive = false;
        this.retiredAt = LocalDateTime.now();
    }
}

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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/*
 * Store one-time pre-keys that are consumed upon delivering PreKeyBundle to receivers.
 */

@Entity
@Table(
    name = "signal_one_time_pre_keys",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_signal_one_time_pre_keys_user_prekey", columnNames = {"user_id", "pre_key_id"})
    },
    indexes = {
        @Index(name = "idx_signal_one_time_pre_keys_user_consumed", columnList = "user_id, consumed_at")
    }
)
public class SignalOneTimePreKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Logic id on Signal protocol, not the database id
    @Column(name = "pre_key_id", nullable = false)
    private int preKeyId;

    // Public key
    @Lob
    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;


    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    public SignalOneTimePreKey() {
        // Default constructor for JPA
    }

    public SignalOneTimePreKey(int preKeyId, byte[] publicKey, User user) {
        this.preKeyId = preKeyId;
        this.publicKey = publicKey;
        this.user = user;
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

    public void SetUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

     public void SetConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

     public LocalDateTime GetConsumedAt() {
        return this.consumedAt;
    }
}

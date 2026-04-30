package com.tfg.backend.SignalEnvelope;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Check;
import org.springframework.format.annotation.DateTimeFormat;

import com.tfg.backend.GroupChat.GroupChat;
import com.tfg.backend.OneToOneChat.OneToOneChat;
import com.tfg.backend.User.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.validation.constraints.AssertTrue;

@Entity
@Table(
    name = "signal_envelopes",
    indexes = {
        @Index(name = "idx_signal_envelopes_pending", columnList = "recipient_user_id, delivered_at, id")
    }
)
@Check(constraints = "((conversation_type = 'DIRECT' AND one_to_one_chat_id IS NOT NULL AND group_chat_id IS NULL) OR (conversation_type = 'GROUP' AND group_chat_id IS NOT NULL AND one_to_one_chat_id IS NULL))")
public class SignalEnvelope {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private User receiver;

    public enum ConversationType {
        DIRECT("DIRECT"), // Equivalent in Signal to One-to-One chats 
        GROUP("GROUP");

        private final String value;

        private ConversationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false)
    private ConversationType conversationType;

    public enum MessageStatus {
        PENDING("PENDING"),
        DELIVERED("DELIVERED");
    
        private final String value;

        private MessageStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_chat_id")
    private GroupChat groupChat; // Only set if ConversationType is GROUP

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "one_to_one_chat_id")
    private OneToOneChat oneToOneChat; // Only set if ConversationType is DIRECT

    @Column(name = "client_message_id")
    private UUID clientMessageId = null; // Optional, for idempotency and tracking duplicate deliveries

    @Column(name = "ciphertext_type", nullable = false)
    private Short cypherTextType;

    @Lob
    @Column(name = "ciphertext", nullable = false)
    private byte[] cypherText;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "read_at")
    private LocalDateTime readAt;

    public SignalEnvelope() {
        // Default constructor for JPA
    }
    
    // OneToOneChats and GroupChats are different classes so we dont have to check the type, only assign it for libsignal
    public SignalEnvelope(User sender, User receiver, GroupChat groupChat, byte[] cypherText, Short cypherTextType) {
        this.sender = sender;
        this.receiver = receiver;
        this.groupChat = groupChat;
        this.cypherText = cypherText;
        this.cypherTextType = cypherTextType;
        this.createdAt = LocalDateTime.now();
        this.conversationType = ConversationType.GROUP;
        this.status = MessageStatus.PENDING;
    }

    public SignalEnvelope(User sender, User receiver, OneToOneChat oneToOneChat, byte[] cypherText, Short cypherTextType) {
        this.sender = sender;
        this.receiver = receiver;
        this.oneToOneChat = oneToOneChat;
        this.cypherText = cypherText;
        this.cypherTextType = cypherTextType;
        this.createdAt = LocalDateTime.now();
        this.conversationType = ConversationType.DIRECT;
        this.status = MessageStatus.PENDING;
    }

    @AssertTrue(message = "Inconsistent conversation type and chat relation")
    public boolean IsConversationConsistent() {
        if (this.conversationType == null) {
            return false;
        }
        if (this.conversationType == ConversationType.DIRECT) {
            return this.oneToOneChat != null && this.groupChat == null;
        }
        return this.groupChat != null && this.oneToOneChat == null;
    }

    public Long getId() {
        return this.id;
    }

    public User getSender() {
        return this.sender;
    }

    public void SetSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return this.receiver;
    }

    public void SetReceiver(User receiver) {
        this.receiver = receiver;
    }

    public String getConversationType() {
        return this.conversationType.getValue();
    }

    public GroupChat getGroupChat() {
        return this.groupChat;
    }

    public void SetGroupChat(GroupChat groupChat) {
        this.groupChat = groupChat;
    }

    public OneToOneChat getOneToOneChat() {
        return this.oneToOneChat;
    }

    public void SetOneToOneChat(OneToOneChat oneToOneChat) {
        this.oneToOneChat = oneToOneChat;
    }

    public byte[] getCypherText() {
        return this.cypherText;
    }

    public void SetCypherText(byte[] cypherText) {
        this.cypherText = cypherText;
    }

    public int getCypherTextType() {
        return this.cypherTextType;
    }

    public UUID getClientMessageId() {
        return this.clientMessageId;
    }

    public void SetClientMessageId(UUID clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getDeliveredAt() {
        return this.deliveredAt;
    }

    public void SetDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getReadAt() {
        return this.readAt;
    }

    public void MarkAsRead() {
        this.readAt = LocalDateTime.now();
    }

    public MessageStatus getStatus(){
        return status;
    }

    public void setValue(MessageStatus status){
        this.status = status;
    }
}

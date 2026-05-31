package com.tfg.backend.SignalEnvelope.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Petición para distribuir un SenderKeyDistributionMessage (SKDM) a un miembro del grupo.
 * El ciphertext va cifrado 1:1 (sesión Signal entre emisor y receptor), por eso no
 * necesita un conversationId de OneToOneChat: el canal lo identifica el groupChatId.
 */
public class SignalGroupSenderKeyRequestDto {
    @NotNull
    private Long recipientUserId;

    @NotNull
    private Long groupChatId;

    @NotNull
    private Short cypherTextType;

    @NotNull
    private String cypherTextB64;

    public SignalGroupSenderKeyRequestDto() {
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public Long getGroupChatId() {
        return groupChatId;
    }

    public Short getCypherTextType() {
        return cypherTextType;
    }

    public String getCypherTextB64() {
        return cypherTextB64;
    }
}

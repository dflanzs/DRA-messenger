package com.tfg.backend.Cypher.dto;

public class SignalBootstrapResponseDto {
    private final int activeSignedPreKeyId;
    private final int activeKyberPreKeyId;
    private final int oneTimePreKeysStored;

    public SignalBootstrapResponseDto(
        int activeSignedPreKeyId,
        int activeKyberPreKeyId,
        int oneTimePreKeysStored
    ) {
        this.activeSignedPreKeyId = activeSignedPreKeyId;
        this.activeKyberPreKeyId = activeKyberPreKeyId;
        this.oneTimePreKeysStored = oneTimePreKeysStored;
    }
}

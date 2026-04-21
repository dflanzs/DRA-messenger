package com.tfg.backend.Cypher.dto;

public class SignalBootstrapResponseDto {
    private Integer activeSignedPreKeyId;
    private Integer activeKyberPreKeyId;
    private Integer oneTimePreKeysStored;

    public SignalBootstrapResponseDto() {
    }

    public SignalBootstrapResponseDto(
        Integer activeSignedPreKeyId,
        Integer activeKyberPreKeyId,
        Integer oneTimePreKeysStored
    ) {
        this.activeSignedPreKeyId = activeSignedPreKeyId;
        this.activeKyberPreKeyId = activeKyberPreKeyId;
        this.oneTimePreKeysStored = oneTimePreKeysStored;
    }

    public Integer getActiveSignedPreKeyId() {
        return activeSignedPreKeyId;
    }

    public void setActiveSignedPreKeyId(Integer activeSignedPreKeyId) {
        this.activeSignedPreKeyId = activeSignedPreKeyId;
    }

    public Integer getActiveKyberPreKeyId() {
        return activeKyberPreKeyId;
    }

    public void setActiveKyberPreKeyId(Integer activeKyberPreKeyId) {
        this.activeKyberPreKeyId = activeKyberPreKeyId;
    }

    public Integer getOneTimePreKeysStored() {
        return oneTimePreKeysStored;
    }

    public void setOneTimePreKeysStored(Integer oneTimePreKeysStored) {
        this.oneTimePreKeysStored = oneTimePreKeysStored;
    }
}

package com.tfg.backend.Cypher.dto;

public class SignalRefillResponseDto {
    private int oneTimePreKeysStored;
    private int oneTimePreKeysRemaining;

    public SignalRefillResponseDto() {
    }

    public SignalRefillResponseDto(int oneTimePreKeysStored, int oneTimePreKeysRemaining) {
        this.oneTimePreKeysStored = oneTimePreKeysStored;
        this.oneTimePreKeysRemaining = oneTimePreKeysRemaining;
    }
}

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

    public int getOneTimePreKeysStored() {
        return oneTimePreKeysStored;
    }

    public void setOneTimePreKeysStored(int oneTimePreKeysStored) {
        this.oneTimePreKeysStored = oneTimePreKeysStored;
    }

    public int getOneTimePreKeysRemaining() {
        return oneTimePreKeysRemaining;
    }

    public void setOneTimePreKeysRemaining(int oneTimePreKeysRemaining) {
        this.oneTimePreKeysRemaining = oneTimePreKeysRemaining;
    }
}

package com.tfg.backend.Cypher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SignalOneTimePreKeyDto {
    @NotNull
    private Integer preKeyId;

    @NotBlank
    private String publicKeyB64;

    public SignalOneTimePreKeyDto() {
    }

    public SignalOneTimePreKeyDto(Integer preKeyId, String publicKeyB64) {
        this.preKeyId = preKeyId;
        this.publicKeyB64 = publicKeyB64;
    }

    public Integer getPreKeyId() {
        return preKeyId;
    }

    public void setPreKeyId(Integer preKeyId) {
        this.preKeyId = preKeyId;
    }

    public String getPublicKeyB64() {
        return publicKeyB64;
    }

    public void setPublicKeyB64(String publicKeyB64) {
        this.publicKeyB64 = publicKeyB64;
    }
}

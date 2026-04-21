package com.tfg.backend.Cypher.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class SignalBootstrapRequestDto {
    @NotNull
    private Integer registrationId;

    @NotBlank
    private String identityKeyPublicB64;

    @NotNull
    private Integer signedPreKeyId;

    @NotBlank
    private String signedPreKeyPublicB64;

    @NotBlank
    private String signedPreKeySignatureB64;

    @NotNull
    private Integer kyberPreKeyId;

    @NotBlank
    private String kyberPreKeyPublicB64;

    @NotBlank
    private String kyberPreKeySignatureB64;

    @Valid
    @NotEmpty
    private List<SignalOneTimePreKeyDto> oneTimePreKeys;

    public SignalBootstrapRequestDto() {
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }

    public String getIdentityKeyPublicB64() {
        return identityKeyPublicB64;
    }

    public void setIdentityKeyPublicB64(String identityKeyPublicB64) {
        this.identityKeyPublicB64 = identityKeyPublicB64;
    }

    public Integer getSignedPreKeyId() {
        return signedPreKeyId;
    }

    public void setSignedPreKeyId(Integer signedPreKeyId) {
        this.signedPreKeyId = signedPreKeyId;
    }

    public String getSignedPreKeyPublicB64() {
        return signedPreKeyPublicB64;
    }

    public void setSignedPreKeyPublicB64(String signedPreKeyPublicB64) {
        this.signedPreKeyPublicB64 = signedPreKeyPublicB64;
    }

    public String getSignedPreKeySignatureB64() {
        return signedPreKeySignatureB64;
    }

    public void setSignedPreKeySignatureB64(String signedPreKeySignatureB64) {
        this.signedPreKeySignatureB64 = signedPreKeySignatureB64;
    }

    public Integer getKyberPreKeyId() {
        return kyberPreKeyId;
    }

    public void setKyberPreKeyId(Integer kyberPreKeyId) {
        this.kyberPreKeyId = kyberPreKeyId;
    }

    public String getKyberPreKeyPublicB64() {
        return kyberPreKeyPublicB64;
    }

    public void setKyberPreKeyPublicB64(String kyberPreKeyPublicB64) {
        this.kyberPreKeyPublicB64 = kyberPreKeyPublicB64;
    }

    public String getKyberPreKeySignatureB64() {
        return kyberPreKeySignatureB64;
    }

    public void setKyberPreKeySignatureB64(String kyberPreKeySignatureB64) {
        this.kyberPreKeySignatureB64 = kyberPreKeySignatureB64;
    }

    public List<SignalOneTimePreKeyDto> getOneTimePreKeys() {
        return oneTimePreKeys;
    }

    public void setOneTimePreKeys(List<SignalOneTimePreKeyDto> oneTimePreKeys) {
        this.oneTimePreKeys = oneTimePreKeys;
    }
}

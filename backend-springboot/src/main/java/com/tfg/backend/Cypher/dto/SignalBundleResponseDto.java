package com.tfg.backend.Cypher.dto;

public class SignalBundleResponseDto {
    private final int registrationId;

    // In a real implementation, this would be a list of devices.
    private final int deviceId = 1;

    private final int oneTimePreKeyId;

    private final String oneTimePreKeyPublicB64;

    private final int signedPreKeyId;

    private final String signedPreKeyPublicB64;

    private final String signedPreKeySignatureB64;

    private final String identityKeyPublicB64;

    private final int kyberPreKeyId;

    private final String kyberPreKeyPublicB64;

    private final String kyberPreKeySignatureB64;

    public SignalBundleResponseDto(
        int registrationId,
        int oneTimePreKeyId,
        String oneTimePreKeyPublicB64,
        int signedPreKeyId,
        String signedPreKeyPublicB64,
        String signedPreKeySignatureB64,
        String identityKeyPublicB64,
        int kyberPreKeyId,
        String kyberPreKeyPublicB64,
        String kyberPreKeySignatureB64
    ) {
        this.registrationId = registrationId;
        this.oneTimePreKeyId = oneTimePreKeyId;
        this.oneTimePreKeyPublicB64 = oneTimePreKeyPublicB64;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublicB64 = signedPreKeyPublicB64;
        this.signedPreKeySignatureB64 = signedPreKeySignatureB64;
        this.identityKeyPublicB64 = identityKeyPublicB64;
        this.kyberPreKeyId = kyberPreKeyId;
        this.kyberPreKeyPublicB64 = kyberPreKeyPublicB64;
        this.kyberPreKeySignatureB64 = kyberPreKeySignatureB64;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getOneTimePreKeyId() {
        return oneTimePreKeyId;
    }

    public String getOneTimePreKeyPublicB64() {
        return oneTimePreKeyPublicB64;
    }

    public int getSignedPreKeyId() {
        return signedPreKeyId;
    }

    public String getSignedPreKeyPublicB64() {
        return signedPreKeyPublicB64;
    }

    public String getSignedPreKeySignatureB64() {
        return signedPreKeySignatureB64;
    }

    public String getIdentityKeyPublicB64() {
        return identityKeyPublicB64;
    }

    public int getKyberPreKeyId() {
        return kyberPreKeyId;
    }

    public String getKyberPreKeyPublicB64() {
        return kyberPreKeyPublicB64;
    }

    public String getKyberPreKeySignatureB64() {
        return kyberPreKeySignatureB64;
    }
}

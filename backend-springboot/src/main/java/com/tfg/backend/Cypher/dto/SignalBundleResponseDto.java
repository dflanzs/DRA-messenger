package com.tfg.backend.Cypher.dto;

public class SignalBundleResponseDto {
    private final int registrationId;

    // In a real implementation, this would be a list of devices.
    private final int deviceId = 1;

    private final int preKeyId;

    private final String preKeyPublicB64; 

    private final int signedPreKeyId;

    private final String signedPreKeyPublicB64;

    private final String signedPreKeySignatureB64;

    private final String identityKeyB64;

    private final int kyberPreKeyId;

    private final String kyberPreKeyPublicB64;

    private final String kyberPreKeySignatureB64;

    public SignalBundleResponseDto(
        int registrationId,
        int preKeyId,
        String preKeyPublicB64,
        int signedPreKeyId,
        String signedPreKeyPublicB64,
        String signedPreKeySignatureB64,
        String identityKeyB64,
        int kyberPreKeyId,
        String kyberPreKeyPublicB64,
        String kyberPreKeySignatureB64
    ) {
        this.registrationId = registrationId;
        this.preKeyId = preKeyId;
        this.preKeyPublicB64 = preKeyPublicB64;
        this.signedPreKeyId = signedPreKeyId;
        this.signedPreKeyPublicB64 = signedPreKeyPublicB64;
        this.signedPreKeySignatureB64 = signedPreKeySignatureB64;
        this.identityKeyB64 = identityKeyB64;
        this.kyberPreKeyId = kyberPreKeyId;
        this.kyberPreKeyPublicB64 = kyberPreKeyPublicB64;
        this.kyberPreKeySignatureB64 = kyberPreKeySignatureB64;
    }
}

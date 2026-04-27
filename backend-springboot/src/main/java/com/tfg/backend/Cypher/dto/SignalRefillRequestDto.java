package com.tfg.backend.Cypher.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class SignalRefillRequestDto {

    @NotNull
    @Valid
    private List<SignalOneTimePreKeyDto> oneTimePreKeys;

    public SignalRefillRequestDto() {
    }

    public List<SignalOneTimePreKeyDto> getOneTimePreKeys() {
        return oneTimePreKeys;
    }
}

package com.tfg.backend.Cypher;

import com.tfg.backend.Cypher.dto.SignalBootstrapRequestDto;
import com.tfg.backend.Cypher.dto.SignalBootstrapResponseDto;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/signal")
public class SignalController {
    private final SignalService signalService;

    public SignalController(SignalService signalService) {
        this.signalService = signalService;
    }


    @PostMapping("/keys/bootstrap/{userId}")
    @PreAuthorize("@authorizationService.isSelf(authentication, #userId)")
    public SignalBootstrapResponseDto postKeysBootstrap(
            @PathVariable Long userId,
            @Valid @RequestBody SignalBootstrapRequestDto request,
            Principal principal
    ) {
        SignalBootstrapResponseDto response = signalService.replaceKeysBootstrap(userId, request);
        return response;
    }
}

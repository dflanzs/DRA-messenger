package com.tfg.backend.Cypher;

import com.tfg.backend.Cypher.dto.SignalBootstrapRequestDto;
import com.tfg.backend.Cypher.dto.SignalBootstrapResponseDto;
import com.tfg.backend.Cypher.dto.SignalBundleResponseDto;
import com.tfg.backend.Cypher.dto.SignalRefillRequestDto;
import com.tfg.backend.Cypher.dto.SignalRefillResponseDto;
import com.tfg.backend.User.User;
import com.tfg.backend.User.UserService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/signal")
public class SignalController {
    private final SignalService signalService;
    private final UserService userService;

    public SignalController(SignalService signalService, UserService userService) {
        this.signalService = signalService;
        this.userService = userService;
    }


    @PostMapping("/keys/bootstrap")
    public SignalBootstrapResponseDto postKeysBootstrap(
            @Valid @RequestBody SignalBootstrapRequestDto request,
            Principal principal
    ) {
        User user = userService.getByEmail(principal.getName());
        SignalBootstrapResponseDto response = signalService.replaceKeysBootstrap(user.getId(), request);
        return response;
    }

    @PostMapping("/keys/one-time/refill")
    public SignalRefillResponseDto postOneTimePreKeysRefill(
            SignalRefillRequestDto request,
            Principal principal
    ) {
        User user = userService.getByEmail(principal.getName());
        SignalRefillResponseDto response = signalService.refillOneTimePreKeys(user.getId(), request);

        return response;
    }

    @GetMapping("/users/{userId}/bundle")
    @PreAuthorize("@authorizationService.isSelf(authentication, #userId)")
    public SignalBundleResponseDto getUserBundle(
            @PathVariable Long userId
    ) {
        SignalBundleResponseDto response = signalService.getUserBundle(userId);
        return response;
    }
}

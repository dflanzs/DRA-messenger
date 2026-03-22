package com.tfg.backend.TrustCircles;

import com.tfg.backend.TrustCircles.dto.CreateTrustCircleDto;
import com.tfg.backend.TrustCircles.dto.CrossConsentDto;
import com.tfg.backend.User.UserService;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/trust-circles")
public class TrustCirclesController {

    private final TrustCirclesService trustCirclesService;
    private final UserService userService;

    public TrustCirclesController(TrustCirclesService trustCirclesService, UserService userService) {
        this.trustCirclesService = trustCirclesService;
        this.userService = userService;
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public List<TrustCircles> list() {
        return trustCirclesService.list();
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<TrustCircles> get(@PathVariable Long id) {
        return ResponseEntity.ok(trustCirclesService.getById(id));
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        TrustCircles circle = trustCirclesService.getById(id);
        circle.setDeletedAt(java.time.LocalDateTime.now());
        trustCirclesService.save(circle);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #userId)")
    @GetMapping("/users/{userId}")
    public List<TrustCircles> getUserCircles(@PathVariable Long userId, Principal principal) {
        return trustCirclesService.getUserCircles(userId);
    }

    // Solo admin porque para usuarios será otro endpoint
    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @PostMapping
    public ResponseEntity<TrustCircles> create(@RequestBody CreateTrustCircleDto dto,
                                               Principal principal) {
        Long creatorId = getAuthenticatedUserId(principal);
        java.util.List<Long> userIds = dto.getUserIds() != null
            ? new java.util.ArrayList<>(dto.getUserIds())
            : new java.util.ArrayList<>();
        if (!userIds.contains(creatorId)) {
            userIds.add(0, creatorId);
        }
        TrustCircles circle = trustCirclesService.createCircle(dto.getName(), userIds);
        return ResponseEntity.created(URI.create("/api/trust-circles/" + circle.getId())).body(circle);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{circleId}/users/{userId}")
    public ResponseEntity<TrustCircles> addUser(@PathVariable Long circleId, @PathVariable Long userId,
                                                Principal principal) {
        Long currentUserId = getAuthenticatedUserId(principal);
        trustCirclesService.validateCircleMembership(circleId, currentUserId);
        return ResponseEntity.ok(trustCirclesService.addUserToCircle(circleId, userId));
    }

    @PreAuthorize("@authorizationService.isSelfOrAdmin(authentication, #userId)")
    @DeleteMapping("/{circleId}/users/{userId}")
    public ResponseEntity<TrustCircles> removeUser(@PathVariable Long circleId, @PathVariable Long userId,
                                                   Principal principal) {
        Long currentUserId = getAuthenticatedUserId(principal);
        trustCirclesService.validateCircleMembership(circleId, currentUserId);
        return ResponseEntity.ok(trustCirclesService.removeUserFromCircle(circleId, userId));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/consents")
    public ResponseEntity<TrustCircles> grantCrossConsent(@RequestBody CrossConsentDto dto,
                                                          Principal principal) {
        Long requesterId = getAuthenticatedUserId(principal);
        TrustCircles consentDomain = trustCirclesService.grantCrossCircleConsent(requesterId, dto.getTargetUserId());
        return ResponseEntity.ok(consentDomain);
    }

    @PreAuthorize("@authorizationService.isSelf(authentication, #userId)")
    @GetMapping("/can-communicate/{userId}")
    public ResponseEntity<Map<String, Boolean>> canCommunicate(
        @PathVariable Long userId,
        @RequestParam Long userId2,
        Principal principal
    ) {
        boolean canCommunicate = trustCirclesService.canUsersCommunicate(userId, userId2);
        return ResponseEntity.ok(Map.of("allowed", canCommunicate));
    }

    private Long getAuthenticatedUserId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return userService.getByEmail(principal.getName()).getId();
    }
}

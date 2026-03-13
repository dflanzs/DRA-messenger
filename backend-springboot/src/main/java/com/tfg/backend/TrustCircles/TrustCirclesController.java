package com.tfg.backend.TrustCircles;

import com.tfg.backend.TrustCircles.dto.CreateTrustCircleDto;
import com.tfg.backend.TrustCircles.dto.CrossConsentDto;
import com.tfg.backend.User.UserService;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public List<TrustCircles> list() {
        return trustCirclesService.list();  // any authenticated user; scoped by ownership in future
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrustCircles> get(@PathVariable Long id) {
        return ResponseEntity.ok(trustCirclesService.getById(id));
    }

    @GetMapping("/users/{userId}")
    public List<TrustCircles> getUserCircles(@PathVariable Long userId, Principal principal) {
        Long currentUserId = getAuthenticatedUserId(principal);
        if (!currentUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propios círculos");
        }
        return trustCirclesService.getUserCircles(userId);
    }

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

    @PostMapping("/{circleId}/users/{userId}")
    public ResponseEntity<TrustCircles> addUser(@PathVariable Long circleId, @PathVariable Long userId,
                                                Principal principal) {
        Long currentUserId = getAuthenticatedUserId(principal);
        trustCirclesService.validateCircleMembership(circleId, currentUserId);
        return ResponseEntity.ok(trustCirclesService.addUserToCircle(circleId, userId));
    }

    @DeleteMapping("/{circleId}/users/{userId}")
    public ResponseEntity<TrustCircles> removeUser(@PathVariable Long circleId, @PathVariable Long userId,
                                                   Principal principal) {
        Long currentUserId = getAuthenticatedUserId(principal);
        trustCirclesService.validateCircleMembership(circleId, currentUserId);
        return ResponseEntity.ok(trustCirclesService.removeUserFromCircle(circleId, userId));
    }

    @PostMapping("/consents")
    public ResponseEntity<TrustCircles> grantCrossConsent(@RequestBody CrossConsentDto dto,
                                                          Principal principal) {
        Long requesterId = getAuthenticatedUserId(principal);
        TrustCircles consentDomain = trustCirclesService.grantCrossCircleConsent(requesterId, dto.getTargetUserId());
        return ResponseEntity.ok(consentDomain);
    }

    @GetMapping("/can-communicate")
    public ResponseEntity<Map<String, Boolean>> canCommunicate(
        @RequestParam Long userId1,
        @RequestParam Long userId2,
        Principal principal
    ) {
        Long currentUserId = getAuthenticatedUserId(principal);
        if (!currentUserId.equals(userId1) && !currentUserId.equals(userId2)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Solo puedes consultar tu propia comunicación");
        }
        boolean canCommunicate = trustCirclesService.canUsersCommunicate(userId1, userId2);
        return ResponseEntity.ok(Map.of("allowed", canCommunicate));
    }

    private Long getAuthenticatedUserId(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return userService.getByEmail(principal.getName()).getId();
    }
}

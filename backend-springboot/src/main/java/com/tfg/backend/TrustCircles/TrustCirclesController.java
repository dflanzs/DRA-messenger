package com.tfg.backend.TrustCircles;

import com.tfg.backend.TrustCircles.dto.CreateTrustCircleDto;
import com.tfg.backend.TrustCircles.dto.CrossConsentDto;
import java.net.URI;
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

@RestController
@RequestMapping("/api/trust-circles")
public class TrustCirclesController {

    private final TrustCirclesService trustCirclesService;

    public TrustCirclesController(TrustCirclesService trustCirclesService) {
        this.trustCirclesService = trustCirclesService;
    }

    @GetMapping
    public List<TrustCircles> list() {
        return trustCirclesService.list();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrustCircles> get(@PathVariable Long id) {
        return ResponseEntity.ok(trustCirclesService.getById(id));
    }

    @GetMapping("/users/{userId}")
    public List<TrustCircles> getUserCircles(@PathVariable Long userId) {
        return trustCirclesService.getUserCircles(userId);
    }

    @PostMapping
    public ResponseEntity<TrustCircles> create(@RequestBody CreateTrustCircleDto dto) {
        TrustCircles circle = trustCirclesService.createCircle(dto.getName(), dto.getUserIds());
        return ResponseEntity.created(URI.create("/api/trust-circles/" + circle.getId())).body(circle);
    }

    @PostMapping("/{circleId}/users/{userId}")
    public ResponseEntity<TrustCircles> addUser(@PathVariable Long circleId, @PathVariable Long userId) {
        return ResponseEntity.ok(trustCirclesService.addUserToCircle(circleId, userId));
    }

    @DeleteMapping("/{circleId}/users/{userId}")
    public ResponseEntity<TrustCircles> removeUser(@PathVariable Long circleId, @PathVariable Long userId) {
        return ResponseEntity.ok(trustCirclesService.removeUserFromCircle(circleId, userId));
    }

    @PostMapping("/consents")
    public ResponseEntity<TrustCircles> grantCrossConsent(@RequestBody CrossConsentDto dto) {
        TrustCircles consentDomain = trustCirclesService.grantCrossCircleConsent(dto.getRequesterId(), dto.getTargetUserId());
        return ResponseEntity.ok(consentDomain);
    }

    @GetMapping("/can-communicate")
    public ResponseEntity<Map<String, Boolean>> canCommunicate(
        @RequestParam Long userId1,
        @RequestParam Long userId2
    ) {
        boolean canCommunicate = trustCirclesService.canUsersCommunicate(userId1, userId2);
        return ResponseEntity.ok(Map.of("allowed", canCommunicate));
    }
}

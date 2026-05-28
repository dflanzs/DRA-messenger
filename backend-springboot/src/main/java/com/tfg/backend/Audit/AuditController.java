package com.tfg.backend.Audit;

import com.tfg.backend.Enums.AuditAction;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PreAuthorize("@authorizationService.isAdmin(authentication)")
    @GetMapping
    public List<Audit> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {
        return auditService.list(userId, action, from, to);
    }

}

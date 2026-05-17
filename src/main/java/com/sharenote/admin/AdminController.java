package com.sharenote.admin;

import com.sharenote.admin.dto.AdminUserResponse;
import com.sharenote.admin.dto.PermanentBanRequest;
import com.sharenote.admin.dto.TemporaryBanRequest;
import com.sharenote.admin.dto.UnbanRequest;
import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditService;
import com.sharenote.audit.dto.AuditEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getUsers());
    }

    @PatchMapping("/users/{userId}/ban-temporary")
    public ResponseEntity<AdminUserResponse> banTemporarily(
            @PathVariable Long userId,
            @Valid @RequestBody TemporaryBanRequest request
    ) {
        return ResponseEntity.ok(adminService.banTemporarily(userId, request));
    }

    @PatchMapping("/users/{userId}/ban-permanent")
    public ResponseEntity<AdminUserResponse> banPermanently(
            @PathVariable Long userId,
            @Valid @RequestBody PermanentBanRequest request
    ) {
        return ResponseEntity.ok(adminService.banPermanently(userId, request));
    }

    @PatchMapping("/users/{userId}/unban")
    public ResponseEntity<AdminUserResponse> unban(
            @PathVariable Long userId,
            @Valid @RequestBody UnbanRequest request
    ) {
        return ResponseEntity.ok(adminService.unban(userId, request));
    }

    @GetMapping("/audit-events")
    public ResponseEntity<List<AuditEventResponse>> getAuditEvents(
            @RequestParam(name = "action", required = false) AuditAction action,
            @RequestParam(name = "actorUserId", required = false) Long actorUserId,
            @RequestParam(name = "targetType", required = false) String targetType,
            @RequestParam(name = "targetId", required = false) Long targetId
    ) {
        return ResponseEntity.ok(auditService.search(action, actorUserId, targetType, targetId));
    }
}

package com.sharenote.notification;

import com.sharenote.notification.dto.NotificationResponse;
import com.sharenote.notification.dto.NotificationSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(unreadOnly));
    }

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryResponse> getMyNotificationSummary() {
        return ResponseEntity.ok(notificationService.getMyNotificationSummary());
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long notificationId) {
        return ResponseEntity.ok(notificationService.markRead(notificationId));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<List<NotificationResponse>> markAllRead() {
        return ResponseEntity.ok(notificationService.markAllRead());
    }
}

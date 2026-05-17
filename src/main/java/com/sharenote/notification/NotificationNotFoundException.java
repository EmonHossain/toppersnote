package com.sharenote.notification;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long notificationId) {
        super("Notification not found: " + notificationId);
    }
}

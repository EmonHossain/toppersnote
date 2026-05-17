package com.sharenote.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}

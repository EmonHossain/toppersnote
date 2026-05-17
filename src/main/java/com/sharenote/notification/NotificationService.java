package com.sharenote.notification;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.note.CurrentUserNotFoundException;
import com.sharenote.note.Note;
import com.sharenote.notification.dto.NotificationResponse;
import com.sharenote.notification.dto.NotificationSummaryResponse;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
public class NotificationService implements NotificationPublisher {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            AuditPublisher auditPublisher
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.auditPublisher = auditPublisher;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    @Override
    public void notifyNewNote(Note note) {
        User uploadedBy = note.getUploadedBy();
        List<Notification> notifications = userRepository
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndCurrentYearIgnoreCaseAndCurrentSemesterIgnoreCase(
                        note.getInstitution(),
                        note.getDegreeProgram(),
                        note.getYear(),
                        note.getSemester()
                )
                .stream()
                .filter(user -> !sameUser(user, uploadedBy))
                .map(user -> new Notification(
                        user,
                        uploadedBy,
                        note,
                        NotificationType.NEW_NOTE,
                        "New note added",
                        formatName(uploadedBy) + " added a new note for " + note.getSubjectClass(),
                        Instant.now(clock)
                ))
                .toList();

        notificationRepository.saveAll(notifications);
    }

    @Transactional
    @Override
    public void notifyTakeALook(Note note, User suggestedBy, Collection<User> recipients, String suggestionMessage) {
        List<Notification> notifications = recipients.stream()
                .filter(user -> !sameUser(user, suggestedBy))
                .map(user -> new Notification(
                        user,
                        suggestedBy,
                        note,
                        NotificationType.TAKE_A_LOOK,
                        "Take a look suggested",
                        buildTakeALookMessage(note, suggestedBy, suggestionMessage),
                        Instant.now(clock)
                ))
                .toList();

        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Boolean unreadOnly) {
        User currentUser = getCurrentUser();
        List<Notification> notifications = Boolean.TRUE.equals(unreadOnly)
                ? notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(currentUser.getId())
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId());

        return notifications.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse getMyNotificationSummary() {
        User currentUser = getCurrentUser();
        return new NotificationSummaryResponse(
                notificationRepository.countByRecipientIdAndReadAtIsNull(currentUser.getId())
        );
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        User currentUser = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (!Objects.equals(notification.getRecipient().getId(), currentUser.getId())) {
            throw new NotificationNotFoundException(notificationId);
        }

        notification.markRead(Instant.now(clock));
        Notification savedNotification = notificationRepository.save(notification);
        auditPublisher.publish(
                AuditAction.NOTIFICATION_READ,
                currentUser,
                "NOTIFICATION",
                savedNotification.getId(),
                "Notification marked read"
        );
        return toResponse(savedNotification);
    }

    @Transactional
    public List<NotificationResponse> markAllRead() {
        User currentUser = getCurrentUser();
        List<Notification> notifications = notificationRepository
                .findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(currentUser.getId());
        Instant readAt = Instant.now(clock);
        notifications.forEach(notification -> notification.markRead(readAt));
        List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
        auditPublisher.publish(
                AuditAction.NOTIFICATIONS_READ_ALL,
                currentUser,
                "NOTIFICATION",
                null,
                "All unread notifications marked read",
                "count=" + savedNotifications.size()
        );
        return savedNotifications
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotFoundException();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private boolean sameUser(User first, User second) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getId() != null && second.getId() != null) {
            return Objects.equals(first.getId(), second.getId());
        }
        return first.getEmail() != null && first.getEmail().equalsIgnoreCase(second.getEmail());
    }

    private String buildTakeALookMessage(Note note, User suggestedBy, String suggestionMessage) {
        String baseMessage = formatName(suggestedBy) + " suggested you take a look at a note for "
                + note.getSubjectClass();
        if (!StringUtils.hasText(suggestionMessage)) {
            return baseMessage;
        }
        return baseMessage + ": " + suggestionMessage.trim();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getNote().getId(),
                notification.getNote().getSubjectClass(),
                notification.getActor().getId(),
                formatName(notification.getActor()),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private String formatName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}

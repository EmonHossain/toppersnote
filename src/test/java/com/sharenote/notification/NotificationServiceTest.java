package com.sharenote.notification;

import com.sharenote.note.Note;
import com.sharenote.notification.dto.NotificationResponse;
import com.sharenote.notification.dto.NotificationSummaryResponse;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "amina@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notifyNewNoteCreatesNotificationsForOtherUsersOnly() {
        User uploader = user(1L, "Amina", "Rahman", "amina@example.com");
        User recipient = user(2L, "Karim", "Said", "karim@example.com");
        Note note = note(10L, uploader);

        when(userRepository.findAll()).thenReturn(List.of(uploader, recipient));
        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyNewNote(note);

        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = stream(captor.getValue());

        assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getActor()).isSameAs(uploader);
        assertThat(notification.getType()).isEqualTo(NotificationType.NEW_NOTE);
        assertThat(notification.getMessage()).contains("Mathematics");
    }

    @Test
    void notifyTakeALookCreatesTargetedMentionNotifications() {
        User sender = user(1L, "Amina", "Rahman", "amina@example.com");
        User recipient = user(2L, "Karim", "Said", "karim@example.com");
        Note note = note(10L, sender);

        when(notificationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.notifyTakeALook(note, sender, List.of(recipient), "Review this before class");

        ArgumentCaptor<Iterable<Notification>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<Notification> notifications = stream(captor.getValue());

        assertThat(notifications).hasSize(1);
        Notification notification = notifications.get(0);
        assertThat(notification.getRecipient()).isSameAs(recipient);
        assertThat(notification.getType()).isEqualTo(NotificationType.TAKE_A_LOOK);
        assertThat(notification.getMessage()).contains("Review this before class");
    }

    @Test
    void getMyNotificationsReturnsUnreadOnlyWhenRequested() {
        User currentUser = user(1L, "Amina", "Rahman", "amina@example.com");
        User actor = user(2L, "Karim", "Said", "karim@example.com");
        Notification notification = notification(100L, currentUser, actor, note(10L, actor), NotificationType.NEW_NOTE);

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getMyNotifications(true);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).type()).isEqualTo("NEW_NOTE");
        assertThat(responses.get(0).read()).isFalse();
        verify(notificationRepository).findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(1L);
    }

    @Test
    void getMyNotificationSummaryReturnsUnreadCount() {
        User currentUser = user(1L, "Amina", "Rahman", "amina@example.com");

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(1L)).thenReturn(4L);

        NotificationSummaryResponse response = notificationService.getMyNotificationSummary();

        assertThat(response.unreadCount()).isEqualTo(4);
    }

    @Test
    void markReadRejectsNotificationOwnedByAnotherUser() {
        User currentUser = user(1L, "Amina", "Rahman", "amina@example.com");
        User otherUser = user(2L, "Karim", "Said", "karim@example.com");
        Notification notification = notification(
                100L,
                otherUser,
                currentUser,
                note(10L, currentUser),
                NotificationType.TAKE_A_LOOK
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markRead(100L))
                .isInstanceOf(NotificationNotFoundException.class)
                .hasMessage("Notification not found: 100");
    }

    @Test
    void markReadSetsReadAtForCurrentUsersNotification() {
        User currentUser = user(1L, "Amina", "Rahman", "amina@example.com");
        User actor = user(2L, "Karim", "Said", "karim@example.com");
        Notification notification = notification(100L, currentUser, actor, note(10L, actor), NotificationType.NEW_NOTE);

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.findById(100L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = notificationService.markRead(100L);

        assertThat(response.read()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    private List<Notification> stream(Iterable<Notification> notifications) {
        return StreamSupport.stream(notifications.spliterator(), false).toList();
    }

    private Notification notification(
            Long id,
            User recipient,
            User actor,
            Note note,
            NotificationType type
    ) {
        Notification notification = new Notification(
                recipient,
                actor,
                note,
                type,
                "Title",
                "Message",
                Instant.parse("2026-05-18T10:15:30Z")
        );
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    private Note note(Long id, User uploadedBy) {
        Note note = new Note(
                "Mathematics",
                "3",
                "2026",
                "calculus.pdf",
                "stored.pdf",
                "application/pdf",
                123,
                "uploads/notes/stored.pdf",
                uploadedBy,
                Instant.parse("2026-05-18T10:15:30Z")
        );
        ReflectionTestUtils.setField(note, "id", id);
        return note;
    }

    private User user(Long id, String firstName, String lastName, String email) {
        User user = new User(
                firstName,
                null,
                lastName,
                email,
                "hashed-password",
                "university",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

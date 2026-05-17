package com.sharenote.audit;

import com.sharenote.audit.dto.AuditEventResponse;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Test
    void publishStoresActorAndTargetContext() {
        AuditService auditService = new AuditService(auditRepository);
        User actor = user();

        auditService.publish(AuditAction.NOTE_UPLOADED, actor, "NOTE", 10L, "Note uploaded", "subject=Math");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditRepository).save(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getAction()).isEqualTo(AuditAction.NOTE_UPLOADED);
        assertThat(event.getActorUserId()).isEqualTo(1L);
        assertThat(event.getActorEmail()).isEqualTo("amina@example.com");
        assertThat(event.getTargetType()).isEqualTo("NOTE");
        assertThat(event.getTargetId()).isEqualTo(10L);
        assertThat(event.getMetadata()).isEqualTo("subject=Math");
    }

    @Test
    void searchByActionReturnsResponses() {
        AuditService auditService = new AuditService(auditRepository);
        AuditEvent event = new AuditEvent(
                AuditAction.USER_TEMPORARILY_BANNED,
                1L,
                "admin@example.com",
                "USER",
                2L,
                "User temporarily banned",
                null,
                java.time.Instant.parse("2026-05-18T10:15:30Z")
        );
        ReflectionTestUtils.setField(event, "id", 100L);

        when(auditRepository.findTop200ByActionOrderByCreatedAtDesc(AuditAction.USER_TEMPORARILY_BANNED))
                .thenReturn(List.of(event));

        List<AuditEventResponse> responses = auditService.search(
                AuditAction.USER_TEMPORARILY_BANNED,
                null,
                null,
                null
        );

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).action()).isEqualTo("USER_TEMPORARILY_BANNED");
        assertThat(responses.get(0).targetId()).isEqualTo(2L);
    }

    private User user() {
        User user = new User(
                "Amina",
                null,
                "Rahman",
                "amina@example.com",
                "hashed-password",
                "university",
                "Computer Science",
                "3",
                "2026",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}

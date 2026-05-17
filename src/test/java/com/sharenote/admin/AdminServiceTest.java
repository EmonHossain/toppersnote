package com.sharenote.admin;

import com.sharenote.admin.dto.AdminUserResponse;
import com.sharenote.admin.dto.PermanentBanRequest;
import com.sharenote.admin.dto.TemporaryBanRequest;
import com.sharenote.admin.dto.UnbanRequest;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, auditPublisher);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void banTemporarilyStoresNoticeReasonAndBanWindow() {
        User admin = user(1L, "Admin", "User", "admin@example.com", Set.of(Role.ADMIN));
        User user = user(2L, "Amina", "Rahman", "amina@example.com", Set.of(Role.USER));

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminUserResponse response = adminService.banTemporarily(
                2L,
                new TemporaryBanRequest(7, "Uploaded policy-violating content", "Your account is paused for review")
        );

        assertThat(response.permanentlyBanned()).isFalse();
        assertThat(response.bannedUntil()).isNotNull();
        assertThat(response.banReason()).isEqualTo("Uploaded policy-violating content");
        assertThat(response.banNotice()).isEqualTo("Your account is paused for review");
        assertThat(response.policyViolationCount()).isEqualTo(1);
    }

    @Test
    void repeatedTemporaryBanEscalatesToPermanentBan() {
        User admin = user(1L, "Admin", "User", "admin@example.com", Set.of(Role.ADMIN));
        User user = user(2L, "Amina", "Rahman", "amina@example.com", Set.of(Role.USER));
        user.banTemporarily(java.time.Instant.now().plusSeconds(60), "first", "first notice");
        user.banTemporarily(java.time.Instant.now().plusSeconds(120), "second", "second notice");

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminUserResponse response = adminService.banTemporarily(
                2L,
                new TemporaryBanRequest(7, "Third policy violation", "Permanent ban notice")
        );

        assertThat(response.permanentlyBanned()).isTrue();
        assertThat(response.bannedUntil()).isNull();
        assertThat(response.policyViolationCount()).isEqualTo(3);
    }

    @Test
    void banPermanentlyMarksUserPermanent() {
        User admin = user(1L, "Admin", "User", "admin@example.com", Set.of(Role.ADMIN));
        User user = user(2L, "Amina", "Rahman", "amina@example.com", Set.of(Role.USER));

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminUserResponse response = adminService.banPermanently(
                2L,
                new PermanentBanRequest("Repeated abuse", "Your account has been permanently banned")
        );

        assertThat(response.permanentlyBanned()).isTrue();
        assertThat(response.banReason()).isEqualTo("Repeated abuse");
    }

    @Test
    void unbanClearsBanStateButKeepsViolationCount() {
        User admin = user(1L, "Admin", "User", "admin@example.com", Set.of(Role.ADMIN));
        User user = user(2L, "Amina", "Rahman", "amina@example.com", Set.of(Role.USER));
        user.banPermanently("Repeated abuse", "Permanent ban notice");

        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        AdminUserResponse response = adminService.unban(2L, new UnbanRequest("Appeal accepted"));

        assertThat(response.permanentlyBanned()).isFalse();
        assertThat(response.bannedUntil()).isNull();
        assertThat(response.banReason()).isNull();
        assertThat(response.banNotice()).isEqualTo("Appeal accepted");
        assertThat(response.policyViolationCount()).isEqualTo(1);
    }

    private User user(Long id, String firstName, String lastName, String email, Set<Role> roles) {
        User user = new User(
                firstName,
                null,
                lastName,
                email,
                "hashed-password",
                "university",
                "Computer Science",
                "3",
                "2026",
                "3",
                "+491234567890",
                "Germany",
                roles
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

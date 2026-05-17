package com.sharenote.admin;

import com.sharenote.admin.dto.AdminUserResponse;
import com.sharenote.admin.dto.PermanentBanRequest;
import com.sharenote.admin.dto.TemporaryBanRequest;
import com.sharenote.admin.dto.UnbanRequest;
import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.note.CurrentUserNotFoundException;
import com.sharenote.user.User;
import com.sharenote.user.UserNotFoundException;
import com.sharenote.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final int TEMPORARY_BAN_ESCALATION_THRESHOLD = 3;

    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public AdminService(UserRepository userRepository, AuditPublisher auditPublisher) {
        this.userRepository = userRepository;
        this.auditPublisher = auditPublisher;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse banTemporarily(Long userId, TemporaryBanRequest request) {
        User admin = getCurrentAdmin();
        User user = getUser(userId);

        if (user.getPolicyViolationCount() + 1 >= TEMPORARY_BAN_ESCALATION_THRESHOLD) {
            user.banPermanently(request.reason().trim(), request.notice().trim());
            User savedUser = userRepository.save(user);
            auditPublisher.publish(
                    AuditAction.USER_PERMANENTLY_BANNED,
                    admin,
                    "USER",
                    savedUser.getId(),
                    "Temporary ban escalated to permanent ban after repeated policy violations",
                    "reason=" + request.reason().trim());
            return toResponse(savedUser);
        }

        Instant bannedUntil = Instant.now(clock).plus(request.durationDays(), ChronoUnit.DAYS);
        user.banTemporarily(bannedUntil, request.reason().trim(), request.notice().trim());
        User savedUser = userRepository.save(user);
        auditPublisher.publish(
                AuditAction.USER_TEMPORARILY_BANNED,
                admin,
                "USER",
                savedUser.getId(),
                "User temporarily banned until " + bannedUntil,
                "reason=" + request.reason().trim());
        return toResponse(savedUser);
    }

    @Transactional
    public AdminUserResponse banPermanently(Long userId, PermanentBanRequest request) {
        User admin = getCurrentAdmin();
        User user = getUser(userId);

        user.banPermanently(request.reason().trim(), request.notice().trim());
        User savedUser = userRepository.save(user);
        auditPublisher.publish(
                AuditAction.USER_PERMANENTLY_BANNED,
                admin,
                "USER",
                savedUser.getId(),
                "User permanently banned",
                "reason=" + request.reason().trim());
        return toResponse(savedUser);
    }

    @Transactional
    public AdminUserResponse unban(Long userId, UnbanRequest request) {
        User admin = getCurrentAdmin();
        User user = getUser(userId);

        user.clearBan(request.notice().trim());
        User savedUser = userRepository.save(user);
        auditPublisher.publish(
                AuditAction.USER_UNBANNED,
                admin,
                "USER",
                savedUser.getId(),
                "User ban cleared");
        return toResponse(savedUser);
    }

    private User getCurrentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotFoundException();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getInstitution(),
                user.getDegreeProgram(),
                user.getCurrentYear(),
                user.getCurrentSemester(),
                user.isPermanentlyBanned(),
                user.getBannedUntil(),
                user.getBanNotice(),
                user.getBanReason(),
                user.getPolicyViolationCount(),
                user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()));
    }
}

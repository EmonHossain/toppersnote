package com.sharenote.admin;

import com.sharenote.admin.dto.AdminUserResponse;
import com.sharenote.admin.dto.PermanentBanRequest;
import com.sharenote.admin.dto.TemporaryBanRequest;
import com.sharenote.admin.dto.UnbanRequest;
import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditRecorder;
import com.sharenote.note.CurrentUserNotFoundException;
import com.sharenote.role.RoleLevel;
import com.sharenote.user.UserNotFoundException;
import com.sharenote.user.UserRepository;
import com.sharenote.user.UserService;
import com.sharenote.user.entities.User;
import com.sharenote.user.entities.UserPolicyStatus;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.relation.Role;

@Service
public class AdminService {

    @Value(value = "${user.policy.temporary_ban_escalation_threshold:3}")
    private final int TEMPORARY_BAN_ESCALATION_THRESHOLD;

    private final UserRepository userRepository;
    private final AuditRecorder auditRecorder;
    private final UserService userService;
    private final Clock clock;

    public AdminService(UserRepository userRepository, AuditRecorder auditRecorder, UserService userService) {
        this.userRepository = userRepository;
        this.auditRecorder = auditRecorder;
        this.userService = userService;
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
    public AdminUserResponse banUserTemporarily(Long userId, TemporaryBanRequest request) {
        if(hasAuthority(userService.getCurrentAuthorities()){

        }
        User admin = getAdmin();
        User user = getUser(userId);
        UserPolicyStatus userPolicy = userService.getUserPolicyStatusForUser(userId);

        if (userPolicy.getPolicyViolationCount() + 1 >= TEMPORARY_BAN_ESCALATION_THRESHOLD) {
            userPolicy.setBanReason(request.reason().trim());
            userPolicy.setBanNotice(request.notice().trim());
            userPolicy.setUser(user);
            userRepository.saveUserPolicy(userPolicy);
            User savedUser = userRepository.save(user);
            auditRecorder.record(
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
        auditRecorder.publish(
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
        auditRecorder.publish(
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
        auditRecorder.publish(
                AuditAction.USER_UNBANNED,
                admin,
                "USER",
                savedUser.getId(),
                "User ban cleared");
        return toResponse(savedUser);
    }

    private User getAdmin() {
        User currectUser = userService.getCurrentUser();
        Set<Role> roles = currectUser.getRoles();
        roles.stream().filter(r-> r.getRoleName().endsWith(null) || r.getRoleName().equals(RoleLevel.SUPER_ADMIN))

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

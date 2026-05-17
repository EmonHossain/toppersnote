package com.sharenote.user;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.storage.ProfilePictureFileStorage;
import com.sharenote.storage.StoredFile;
import com.sharenote.user.dto.RegisterUserRequest;
import com.sharenote.user.dto.UserResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfilePictureFileStorage profilePictureFileStorage;
    private final AuditPublisher auditPublisher;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ProfilePictureFileStorage profilePictureFileStorage,
            AuditPublisher auditPublisher
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.profilePictureFileStorage = profilePictureFileStorage;
        this.auditPublisher = auditPublisher;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = new User(
                request.firstName().trim(),
                normalizeOptional(request.middleName()),
                request.lastName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.institution().trim(),
                request.degreeProgram().trim(),
                request.currentSemesterOrYear().trim(),
                request.currentYear().trim(),
                request.currentSemester().trim(),
                request.phoneNumber().trim(),
                request.country().trim(),
                Set.of(Role.USER)
        );

        User savedUser = userRepository.save(user);
        auditPublisher.publish(AuditAction.USER_REGISTERED, savedUser, "USER", savedUser.getId(), "User registered");
        return toResponse(savedUser);
    }

    @Transactional
    public UserResponse setupProfilePicture(MultipartFile file) {
        User user = getCurrentUser();
        StoredFile previousProfilePicture = toStoredFile(user);
        StoredFile storedFile = profilePictureFileStorage.store(file);

        try {
            user.updateProfilePicture(
                    storedFile.originalFileName(),
                    storedFile.storedFileName(),
                    storedFile.contentType(),
                    storedFile.fileSize(),
                    storedFile.storageKey(),
                    storedFile.storageLocation()
            );
            UserResponse response = toResponse(userRepository.save(user));
            auditPublisher.publish(
                    AuditAction.PROFILE_PICTURE_UPDATED,
                    user,
                    "USER",
                    user.getId(),
                    "Profile picture updated"
            );
            registerProfilePictureCleanup(storedFile, previousProfilePicture);
            return response;
        } catch (RuntimeException exception) {
            profilePictureFileStorage.deleteIfExists(storedFile);
            throw exception;
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getEmail(),
                user.getInstitution(),
                user.getDegreeProgram(),
                user.getCurrentSemesterOrYear(),
                user.getCurrentYear(),
                user.getCurrentSemester(),
                user.getPhoneNumber(),
                user.getCountry(),
                user.getProfilePictureOriginalFileName(),
                user.getProfilePictureContentType(),
                user.getProfilePictureFileSize(),
                user.isPermanentlyBanned(),
                user.getBannedUntil(),
                user.getBanNotice(),
                user.getBanReason(),
                user.getPolicyViolationCount(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotFoundException();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private StoredFile toStoredFile(User user) {
        if (!user.hasProfilePicture()) {
            return null;
        }

        return new StoredFile(
                user.getProfilePictureOriginalFileName(),
                user.getProfilePictureStoredFileName(),
                user.getProfilePictureContentType(),
                user.getProfilePictureFileSize() == null ? 0L : user.getProfilePictureFileSize(),
                user.getProfilePictureStorageKey(),
                user.getProfilePictureStorageLocation()
        );
    }

    private void registerProfilePictureCleanup(StoredFile newProfilePicture, StoredFile previousProfilePicture) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            profilePictureFileStorage.deleteIfExists(previousProfilePicture);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                profilePictureFileStorage.deleteIfExists(previousProfilePicture);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    profilePictureFileStorage.deleteIfExists(newProfilePicture);
                }
            }
        });
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

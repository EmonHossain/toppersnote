package com.sharenote.user;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 100)
    private String middleName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 120)
    private String institution;

    @Column(nullable = false, length = 120)
    private String degreeProgram;

    @Column(nullable = false, length = 50)
    private String currentSemesterOrYear;

    @Column(nullable = false, length = 20)
    private String currentYear;

    @Column(nullable = false, length = 50)
    private String currentSemester;

    @Column(nullable = false, length = 30)
    private String phoneNumber;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 255)
    private String profilePictureOriginalFileName;

    @Column(length = 255)
    private String profilePictureStoredFileName;

    @Column(length = 100)
    private String profilePictureContentType;

    private Long profilePictureFileSize;

    @Column(length = 500)
    private String profilePictureStorageKey;

    @Column(length = 1000)
    private String profilePictureStorageLocation;

    @Column(nullable = false)
    private boolean permanentlyBanned;

    private Instant bannedUntil;

    @Column(length = 1000)
    private String banNotice;

    @Column(length = 1000)
    private String banReason;

    @Column(nullable = false)
    private int policyViolationCount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

    public User(String email, String password, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public User(
            String firstName,
            String middleName,
            String lastName,
            String email,
            String password,
            String institution,
            String degreeProgram,
            String currentSemesterOrYear,
            String currentYear,
            String currentSemester,
            String phoneNumber,
            String country,
            Set<Role> roles
    ) {
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.institution = institution;
        this.degreeProgram = degreeProgram;
        this.currentSemesterOrYear = currentSemesterOrYear;
        this.currentYear = currentYear;
        this.currentSemester = currentSemester;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.roles = roles;
    }

    public boolean hasProfilePicture() {
        return profilePictureStorageLocation != null;
    }

    public void updateProfilePicture(
            String originalFileName,
            String storedFileName,
            String contentType,
            long fileSize,
            String storageKey,
            String storageLocation
    ) {
        this.profilePictureOriginalFileName = originalFileName;
        this.profilePictureStoredFileName = storedFileName;
        this.profilePictureContentType = contentType;
        this.profilePictureFileSize = fileSize;
        this.profilePictureStorageKey = storageKey;
        this.profilePictureStorageLocation = storageLocation;
    }

    public boolean isCurrentlyBanned(Instant now) {
        return permanentlyBanned || (bannedUntil != null && bannedUntil.isAfter(now));
    }

    public void banTemporarily(Instant until, String reason, String notice) {
        this.permanentlyBanned = false;
        this.bannedUntil = until;
        this.banReason = reason;
        this.banNotice = notice;
        this.policyViolationCount++;
    }

    public void banPermanently(String reason, String notice) {
        this.permanentlyBanned = true;
        this.bannedUntil = null;
        this.banReason = reason;
        this.banNotice = notice;
        this.policyViolationCount++;
    }

    public void clearBan(String notice) {
        this.permanentlyBanned = false;
        this.bannedUntil = null;
        this.banNotice = notice;
        this.banReason = null;
    }
}

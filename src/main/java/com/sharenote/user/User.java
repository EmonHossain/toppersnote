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

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
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

    @Column(nullable = false, length = 50)
    private String currentSemesterOrYear;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

    protected User() {
    }

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
            String currentSemesterOrYear,
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
        this.currentSemesterOrYear = currentSemesterOrYear;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getInstitution() {
        return institution;
    }

    public String getCurrentSemesterOrYear() {
        return currentSemesterOrYear;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getCountry() {
        return country;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getProfilePictureOriginalFileName() {
        return profilePictureOriginalFileName;
    }

    public String getProfilePictureStoredFileName() {
        return profilePictureStoredFileName;
    }

    public String getProfilePictureContentType() {
        return profilePictureContentType;
    }

    public Long getProfilePictureFileSize() {
        return profilePictureFileSize;
    }

    public String getProfilePictureStorageKey() {
        return profilePictureStorageKey;
    }

    public String getProfilePictureStorageLocation() {
        return profilePictureStorageLocation;
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
}

package com.sharenote.user.entities;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.sharenote.permission.Permission;
import com.sharenote.role.Role;
import com.sharenote.user.enums.Gender;
import com.sharenote.user.enums.ProfileHealth;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
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

    @Column(nullable = false, unique = true, length = 16)
    private String username;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(name = "gender", nullable = false, columnDefinition = "TINYINT")
    private Gender gender;

    @Column(nullable = true, length = 30)
    private String phoneNumber;

    @Column(nullable = true, length = 100)
    private String country;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean darkModeEnabled;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean emailVerified;

    private Instant emailVerifiedAt;

    @Column(name = "health_code", nullable = false, columnDefinition = "TINYINT DEFAULT 1") // Default to 1 (healthy)
    private ProfileHealth profileHealth;

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private UserPolicyStatus userPolicyStatus;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_roles", // Name of the join table
            joinColumns = @JoinColumn(name = "user_id"), // FK pointing to users table
            inverseJoinColumns = @JoinColumn(name = "role_id")// FK pointing to roles table
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "users_permissions", // Name of the join table
            joinColumns = @JoinColumn(name = "user_id"), // FK pointing to users table
            inverseJoinColumns = @JoinColumn(name = "permission_id")// FK pointing to permissions table
    )
    private Set<Permission> permissions = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private UserResource userResource;

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private StudentUser studentUser;

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private ProfessionalUser professionalProfile;

    // Creates a minimal user for authentication-focused tests.
    public User(String email, String password, Set<Role> roles) {
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    // Creates a fully registered user with academic context and roles.
    public User(
            String firstName,
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
            Set<Role> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.country = country;
        this.roles = roles;
    }

    public User(
            String firstName,
            String lastName,
            String email,
            String password,
            Set<Role> roles) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    public void setProfile(ProfessionalUser professionalProfile) {
        if (professionalProfile == null && this.professionalProfile != null) {
            this.professionalProfile.setUser(null);
        } else if (professionalProfile != null) {
            professionalProfile.setUser(this);
        } else {
            this.professionalProfile = professionalProfile;
        }
    }
}

package com.sharenote.academic;

import com.sharenote.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "class_registrations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_class_registration_user",
                columnNames = {"academic_class_id", "user_id"}
        )
)
public class ClassRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_class_id", nullable = false)
    private AcademicClass academicClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    protected ClassRegistration() {
    }

    public ClassRegistration(AcademicClass academicClass, User user, Instant registeredAt) {
        this.academicClass = academicClass;
        this.user = user;
        this.registeredAt = registeredAt;
    }

    public Long getId() {
        return id;
    }

    public AcademicClass getAcademicClass() {
        return academicClass;
    }

    public User getUser() {
        return user;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}

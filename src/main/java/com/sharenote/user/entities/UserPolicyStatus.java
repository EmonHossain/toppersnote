package com.sharenote.user.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_policy_status")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserPolicyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE" )
    private boolean acceptedTermsOfService;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE" )
    private boolean acceptedPrivacyPolicy;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE" )
    private boolean permanentlyBanned;

    @Column(nullable = true)
    private Instant bannedUntil;

    @Column(nullable = true, length = 1000)
    private String banNotice;

    @Column(nullable = true, length = 1000)
    private String banReason;

    @Column(nullable = false, columnDefinition = "TINYINT DEFAULT 0" )
    private int policyViolationCount;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;
}

package com.sharenote.verification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "email.verification")
@Getter
@Setter
public class EmailVerificationProperties {

    private long tokenExpirationMinutes = 60;
    private String baseUrl = "http://localhost:8080";
    private String apiBasePath = "/api/v1";
    private String delivery = "log";
    private String fromAddress = "no-reply@sharenote.local";
    private String subject = "Verify your ShareNote email";
    private Set<String> allowedDomains = new LinkedHashSet<>();
    private KafkaSettings kafka = new KafkaSettings();

    // Normalizes a null allowed-domain collection during configuration binding.
    public void setAllowedDomains(Set<String> allowedDomains) {
        this.allowedDomains = allowedDomains == null ? new LinkedHashSet<>() : allowedDomains;
    }

    @Getter
    @Setter
    public static class KafkaSettings {

        private String topic = "sharenote.email-verification";
        private String deadLetterTopic = "sharenote.email-verification.dlt";
        private String consumerGroup = "sharenote-email-verification";
        private int partitions = 3;
        private short replicationFactor = 1;
        private long retentionMs = 86_400_000;
        private long retryBackoffMs = 1_000;
        private int retryAttempts = 3;
    }
}

package com.sharenote.analytics;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnalyticsProperties.class)
public class AnalyticsConfig {
}

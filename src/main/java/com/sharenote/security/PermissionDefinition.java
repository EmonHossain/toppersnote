package com.sharenote.security;

import lombok.Data;

@Data
public class PermissionDefinition {
    private final Long id;
    private final String httpMethod;
    private final String urlPattern;
    private final String action;
    private final int order;
}

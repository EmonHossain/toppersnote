package com.sharenote.security;

import java.io.Serializable;

import lombok.Data;

@Data
public class SecurityPermissionCache implements Serializable{
    private long id;
    private String urlPattern;
    private String httpMethod;
    private String requiredPermission;
    private int sortOrder;
}
package com.sharenote.user.dto;

import java.io.Serializable;

public class UserCache implements Serializable {
    private long id;
    private String username;
    private String firstName;
    private String lastName;
    private boolean banned;
    private List<RoleCache> rolles
}

package com.sharenote.user.entities;

import com.sharenote.role.Role;

import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(User.class)
public abstract class User_ {

    public static volatile SingularAttribute<User, Long> id;
    public static volatile SingularAttribute<User, String> username;
    public static volatile SingularAttribute<User, String> firstName;
    public static volatile SingularAttribute<User, String> lastName;
    public static volatile SingularAttribute<User, String> email;
    public static volatile SingularAttribute<User, String> password;
    public static volatile SingularAttribute<User, String> institution;
    public static volatile SingularAttribute<User, String> degreeProgram;
    public static volatile SingularAttribute<User, String> currentSemesterOrYear;
    public static volatile SingularAttribute<User, String> currentYear;
    public static volatile SingularAttribute<User, String> currentSemester;
    public static volatile SingularAttribute<User, String> phoneNumber;
    public static volatile SingularAttribute<User, String> country;
    public static volatile SetAttribute<User, Role> roles;
}

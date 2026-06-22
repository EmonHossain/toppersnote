package com.sharenote.user.enums;

public enum UserType {
    INDIVIDUAL(Values.INDIVIDUAL),
    STUDENT(Values.STUDENT),
    PROFESSIONAL(Values.PROFESSIONAL);

    private final String value;

    UserType(String value) {
        this.value = value;
    }

    public static class Values {
        public static final String INDIVIDUAL = "INDIVIDUAL";
        public static final String STUDENT = "STUDENT";
        public static final String PROFESSIONAL = "PROFESSIONAL";
    }
}
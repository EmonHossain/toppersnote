package com.sharenote.security;

public enum SecAction {
    READ("read"),
    WRITE("write"),
    UPDATE("update"),
    DELETE("delete");

    private String value;

    SecAction(String value) {
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
    
}

package com.sharenote.user.enums;

public enum ProfileCompletionStatus {
    INCOMPLETE(0),
    COMPLETE(1);

    private final int value;

    ProfileCompletionStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Static method to convert an integer value to the corresponding enum constant
    public static ProfileCompletionStatus fromValue(int value) {
        for (ProfileCompletionStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown profile completion status value:" + value);
    }

}

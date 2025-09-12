package ru.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole {
    ADMIN, USER;

    @JsonCreator
    public static UserRole fromString(String status) {
        return UserRole.valueOf(status.toUpperCase());
    }
}

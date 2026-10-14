package ru.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum ServiceType {
    HAIRCUT("Стрижка"),
    BEARD("Уход за бородой"),
    SHAVE("Бритье головы"),
    COLORING("Окрашивание волос"),
    ANOTHER("Другое");

    private final String description;

    ServiceType(String title) {
        this.description = title;
    }

    @JsonCreator
    public static ServiceType fromString(String value) {
        if (value == null) return null;
        for (ServiceType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.getDescription().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Неизвестный тип услуги: " + value);
    }
}

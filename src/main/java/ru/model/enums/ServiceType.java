package ru.model.enums;

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

}

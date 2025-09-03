package ru.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatusAppointment {
    PENDING,
    CONFIRMED,
    CANCELED;

    @JsonCreator
    public static StatusAppointment fromString(String status) {
        return StatusAppointment.valueOf(status.toUpperCase());
    }
}

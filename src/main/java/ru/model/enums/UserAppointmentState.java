package ru.model.enums;

import lombok.Getter;

@Getter
public enum UserAppointmentState {
    STATE_AWAITING_NAME("AWAITING_NAME"),
    STATE_AWAITING_PHONE("AWAITING_PHONE"),
    STATE_AWAITING_DATE("AWAITING_DATE");

    private final String value;

    UserAppointmentState(String value) {
        this.value = value;
    }

    public static UserAppointmentState fromValue(String value) {
        for (UserAppointmentState state : values()) {
            if (state.value.equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown state: " + value);
    }
}

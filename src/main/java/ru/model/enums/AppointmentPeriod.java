package ru.model.enums;

import lombok.Getter;

@Getter
public enum AppointmentPeriod {
    ALL("Активные записи", "Нет активных записей."),
    TODAY("Активные записи на сегодня", "Нет активных записей на сегодня."),
    TOMORROW("Активные записи на завтра", "Нет активных записей на завтра.");

    private final String title;
    private final String emptyMessage;

    AppointmentPeriod(String title, String emptyMessage) {
        this.title = title;
        this.emptyMessage = emptyMessage;
    }

}

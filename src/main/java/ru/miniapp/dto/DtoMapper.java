package ru.miniapp.dto;

import ru.model.Appointment;
import ru.model.LunchBreak;
import ru.model.User;
import ru.model.WorkSchedule;

import java.time.DayOfWeek;
import java.util.Locale;

/**
 * Маппер Entity → DTO для Mini App API.
 * Все методы статические — утилитный класс, не спринг-бин.
 */
public final class DtoMapper {

    private DtoMapper() {}

    // ─── User ────────────────────────────────────────────────────────────────

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getTelegramId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getClientPhoneNumber(),
                user.getRole() != null ? user.getRole().name() : null,
                Boolean.TRUE.equals(user.getIsBlocked())
        );
    }

    // ─── Appointment ─────────────────────────────────────────────────────────

    public static AppointmentResponse toAppointmentResponse(Appointment appointment) {
        User user = appointment.getUser();
        String clientName = buildClientName(user);
        String serviceType = appointment.getServiceType() != null
                ? appointment.getServiceType().name()
                : null; // null допустим — бот-флоу не передаёт тип услуги

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDateTime(),
                appointment.getStatus() != null ? appointment.getStatus().name() : null,
                serviceType,
                clientName,
                user != null ? user.getClientPhoneNumber() : null,
                user != null ? user.getTelegramId() : null
        );
    }

    private static String buildClientName(User user) {
        if (user == null) return "Неизвестно";
        StringBuilder sb = new StringBuilder();
        if (user.getFirstName() != null) sb.append(user.getFirstName());
        if (user.getLastName() != null) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(user.getLastName());
        }
        return sb.isEmpty() ? user.getUsername() : sb.toString();
    }

    // ─── WorkSchedule ─────────────────────────────────────────────────────────

    public static WorkScheduleResponse toWorkScheduleResponse(WorkSchedule ws) {
        String dayName = dayOfWeekName(ws.getDayOfWeek());
        return new WorkScheduleResponse(
                ws.getDayOfWeek(),
                dayName,
                Boolean.TRUE.equals(ws.getIsWorkingDay()),
                ws.getStartTime(),
                ws.getEndTime()
        );
    }

    // ─── LunchBreak ──────────────────────────────────────────────────────────

    public static LunchBreakResponse toLunchBreakResponse(LunchBreak lb) {
        return new LunchBreakResponse(
                lb.getDayOfWeek(),
                lb.getStartTime(),
                lb.getEndTime(),
                Boolean.TRUE.equals(lb.getIsActive())
        );
    }

    // ─── Утилиты ─────────────────────────────────────────────────────────────

    /**
     * Название дня недели на русском.
     * dayOfWeek: 1=Пн, ..., 7=Вс (ISO-8601).
     */
    private static String dayOfWeekName(Integer dayOfWeek) {
        if (dayOfWeek == null) return "";
        return switch (dayOfWeek) {
            case 1 -> "Понедельник";
            case 2 -> "Вторник";
            case 3 -> "Среда";
            case 4 -> "Четверг";
            case 5 -> "Пятница";
            case 6 -> "Суббота";
            case 7 -> "Воскресенье";
            default -> "День " + dayOfWeek;
        };
    }
}

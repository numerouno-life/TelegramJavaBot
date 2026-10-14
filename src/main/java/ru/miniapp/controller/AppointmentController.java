package ru.miniapp.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import ru.miniapp.dto.AppointmentResponse;
import ru.miniapp.dto.CreateAppointmentRequest;
import ru.miniapp.dto.DtoMapper;
import ru.model.Appointment;
import ru.model.User;
import ru.model.enums.ServiceType;
import ru.model.enums.StatusAppointment;
import ru.service.AppointmentService;
import ru.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST-контроллер для работы с записями в Mini App.
 * Вся бизнес-логика — в AppointmentService и UserService.
 */
@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;

    /**
     * GET /api/appointments/my
     * Список всех записей текущего пользователя (все статусы).
     */
    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(Authentication auth) {
        Long telegramId = getTelegramId(auth);
        List<AppointmentResponse> result = appointmentService.getUserAppointments(telegramId)
                .stream()
                .map(DtoMapper::toAppointmentResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/appointments/available-dates?from=YYYY-MM-DD&days=7
     * Доступные даты для записи, начиная с from на days вперёд.
     */
    @GetMapping("/available-dates")
    public ResponseEntity<List<LocalDate>> getAvailableDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(defaultValue = "30") int days) {

        List<LocalDate> dates = appointmentService.getAvailableDates(from, days);
        return ResponseEntity.ok(dates);
    }

    /**
     * GET /api/appointments/available-slots?date=YYYY-MM-DD
     * Доступные временные слоты на конкретный день.
     */
    @GetMapping("/available-slots")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // AppointmentService.getAvailableTimeSlots принимает LocalDateTime
        List<LocalDateTime> slots = appointmentService.getAvailableTimeSlots(date.atStartOfDay());
        return ResponseEntity.ok(slots);
    }

    /**
     * POST /api/appointments
     * Создать новую запись через Mini App. serviceType обязателен.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication auth) {

        Long telegramId = getTelegramId(auth);
        User user = userService.findByTelegramId(telegramId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Пользователь не найден: " + telegramId));

        Appointment appointment = Appointment.builder()
                .user(user)
                .dateTime(request.dateTime())
                .serviceType(request.serviceType())
                .status(StatusAppointment.ACTIVE)
                .build();

        Appointment saved = appointmentService.createAppointment(appointment);
        log.info("Mini App: создана запись id={}, telegramId={}, дата={}, услуга={}",
                saved.getId(), telegramId, saved.getDateTime(), saved.getServiceType());

        return ResponseEntity.ok(DtoMapper.toAppointmentResponse(saved));
    }

    /**
     * DELETE /api/appointments/{id}
     * Отменить запись. Пользователь может отменить только свою.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(
            @PathVariable Long id,
            Authentication auth) {

        Long telegramId = getTelegramId(auth);
        Appointment appointment = appointmentService.findById(id);

        // Проверяем, что запись принадлежит текущему пользователю
        if (!telegramId.equals(appointment.getUser().getTelegramId())) {
            throw new ru.error.exception.AccessDeniedException(
                    "Вы можете отменять только свои записи");
        }

        appointmentService.cancellationNoticeForAdmins(appointment);
        appointmentService.cancelAppointment(id);
        log.info("Mini App: отменена запись id={}, telegramId={}", id, telegramId);

        return ResponseEntity.noContent().build();
    }

    private Long getTelegramId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}

package ru.miniapp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.miniapp.dto.DtoMapper;
import ru.miniapp.dto.LunchBreakResponse;
import ru.miniapp.dto.WorkScheduleResponse;
import ru.service.LunchBreakService;
import ru.service.WorkScheduleService;

import java.util.List;

/**
 * Публичный (для авторизованных) контроллер расписания.
 * Фронт использует эти данные для отображения рабочих часов.
 */
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class WorkScheduleController {

    private final WorkScheduleService workScheduleService;
    private final LunchBreakService lunchBreakService;

    /**
     * GET /api/schedule/work-days
     * Все 7 дней недели с флагом isWorkingDay и временем работы.
     */
    @GetMapping("/work-days")
    public ResponseEntity<List<WorkScheduleResponse>> getWorkDays() {
        List<WorkScheduleResponse> result = workScheduleService.getAllWorkSchedules()
                .stream()
                .map(DtoMapper::toWorkScheduleResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/schedule/lunch-breaks
     * Обеденные перерывы для всех дней (isActive=false — перерыв отключён).
     */
    @GetMapping("/lunch-breaks")
    public ResponseEntity<List<LunchBreakResponse>> getLunchBreaks() {
        List<LunchBreakResponse> result = lunchBreakService.getAllLunchBreaks()
                .stream()
                .map(DtoMapper::toLunchBreakResponse)
                .toList();
        return ResponseEntity.ok(result);
    }
}

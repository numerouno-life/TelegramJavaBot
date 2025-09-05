package ru.error.controller;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.error.exception.AccessDeniedException;
import ru.error.exception.AppointmentNotFoundException;
import ru.error.exception.ValidationException;
import ru.error.model.ApiError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestsException(Exception e) {
        log.warn(e.getMessage(), e);

        String errorMessage = "";
        List<String> errors = new ArrayList<>();
        String reason = "";
        Map<String, Object> context = null;

        if (e instanceof ValidationException ex) {
            errorMessage = ex.getMessage();
            reason = "ValidationException";
            try {
                String[] parts = errorMessage.split(":");
                if (parts.length > 1) {
                    context = Map.of("entityId", parts[1].trim());
                    errorMessage = parts[0].trim();
                } else {
                    // Если сообщение не содержит ":", добавляем всё сообщение в контекст
                    context = Map.of("errorMessage", errorMessage);
                }
            } catch (Exception ignored) {
                // В случае ошибки всё равно добавляем сообщение в контекст
                context = Map.of("errorMessage", errorMessage);
            }
        }
        return ApiError.builder()
                .errors(errors)
                .message(errorMessage)
                .reason(reason)
                .status(HttpStatus.BAD_REQUEST.name())
                .localDateTime(LocalDateTime.now())
                .context(context)
                .build();
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(final AppointmentNotFoundException e) {
        log.warn(e.getMessage(), e);
        List<String> errors = new ArrayList<>();
        return ApiError.builder()
                .errors(errors)
                .message(e.getMessage())
                .reason("AppointmentNotFoundException")
                .status(HttpStatus.NOT_FOUND.name())
                .localDateTime(LocalDateTime.now())
                .build();

    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleAccessDenied(final AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage(), e);
        return ApiError.builder()
                .errors(new ArrayList<>())
                .message(e.getMessage())
                .reason("Access denied")   // Общая причина
                .status(HttpStatus.FORBIDDEN.name())
                .localDateTime(LocalDateTime.now())
                .build();
    }


    private String extractConstraintName(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof ConstraintViolationException cvEx) {
            return cvEx.getConstraintName();
        }
        return "Unknown constraint";
    }


}

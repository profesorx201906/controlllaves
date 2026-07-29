package com.institucion.prestamo_llaves_api.shared.web;


import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.institucion.prestamo_llaves_api.shared.exception.InvalidCredentialsException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Convierte excepciones controladas en respuestas HTTP.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(
        InvalidCredentialsException.class
    )
    public ResponseEntity<ApiError>
            handleInvalidCredentials(
                InvalidCredentialsException exception,
                HttpServletRequest request
            ) {

        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            exception.getCode(),
            exception.getMessage(),
            request.getRequestURI()
        );
    }

    @ExceptionHandler(
        MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiError>
            handleValidation(
                MethodArgumentNotValidException exception,
                HttpServletRequest request
            ) {

        String message = exception
            .getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(error ->
                error.getDefaultMessage()
            )
            .orElse(
                "La solicitud contiene datos inválidos"
            );

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_ERROR",
            message,
            request.getRequestURI()
        );
    }

    private ResponseEntity<ApiError> buildResponse(
            HttpStatus status,
            String code,
            String message,
            String path
    ) {
        ApiError error = new ApiError(
            clock.instant(),
            status.value(),
            code,
            message,
            path
        );

        return ResponseEntity
            .status(status)
            .body(error);
    }
}
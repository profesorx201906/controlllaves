package com.institucion.prestamo_llaves_api.shared.web;

import java.time.Clock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.institucion.prestamo_llaves_api.shared.exception.InvalidCredentialsException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import com.institucion.prestamo_llaves_api.shared.exception.BusinessRuleException;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Convierte excepciones controladas en respuestas HTTP.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

        private final Clock clock;

        public ApiExceptionHandler(Clock clock) {
                this.clock = clock;
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<ApiError> handleInvalidCredentials(
                        InvalidCredentialsException exception,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.UNAUTHORIZED,
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiError> handleValidation(
                        MethodArgumentNotValidException exception,
                        HttpServletRequest request) {

                String message = exception
                                .getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getDefaultMessage())
                                .orElse(
                                                "La solicitud contiene datos inválidos");

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                message,
                                request.getRequestURI());
        }

        private ResponseEntity<ApiError> buildResponse(
                        HttpStatus status,
                        String code,
                        String message,
                        String path) {
                ApiError error = new ApiError(
                                clock.instant(),
                                status.value(),
                                code,
                                message,
                                path);

                return ResponseEntity
                                .status(status)
                                .body(error);
        }

        @ExceptionHandler(InvalidRequestException.class)
        public ResponseEntity<ApiError> handleInvalidRequest(
                        InvalidRequestException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ApiError> handleNotFound(
                        ResourceNotFoundException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.NOT_FOUND,
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(BusinessRuleException.class)
        public ResponseEntity<ApiError> handleBusinessRule(
                        BusinessRuleException exception,
                        HttpServletRequest request) {
                return buildResponse(
                                HttpStatus.CONFLICT,
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI());
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiError> handleUnreadableMessage(
                        HttpMessageNotReadableException exception,
                        HttpServletRequest request) {

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "INVALID_REQUEST_BODY",
                                "El cuerpo de la solicitud contiene un valor inválido",
                                request.getRequestURI());
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiError> handleConstraintViolation(
                        ConstraintViolationException exception,
                        HttpServletRequest request) {

                String message = exception
                                .getConstraintViolations()
                                .stream()
                                .findFirst()
                                .map(violation -> violation.getMessage())
                                .orElse(
                                                "La solicitud contiene parámetros inválidos");

                return buildResponse(
                                HttpStatus.BAD_REQUEST,
                                "VALIDATION_ERROR",
                                message,
                                request.getRequestURI());
        }
}
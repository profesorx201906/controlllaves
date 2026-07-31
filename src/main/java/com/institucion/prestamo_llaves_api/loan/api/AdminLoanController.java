package com.institucion.prestamo_llaves_api.loan.api;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.institucion.prestamo_llaves_api.loan.api.dto.AdminLoanResponse;
import com.institucion.prestamo_llaves_api.loan.application.AdminLoanQueryApplicationService;
import com.institucion.prestamo_llaves_api.loan.application.AdminLoanQueryResult;
import com.institucion.prestamo_llaves_api.shared.web.PagedResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Consulta administrativa global de préstamos.
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/loans")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminLoanController {

    private final AdminLoanQueryApplicationService
        loanQueryService;

    public AdminLoanController(
            AdminLoanQueryApplicationService
                loanQueryService
    ) {
        this.loanQueryService = loanQueryService;
    }

    @GetMapping
    public ResponseEntity<
            PagedResponse<AdminLoanResponse>
        > searchLoans(

        @RequestParam(defaultValue = "0")
        @Min(
            value = 0,
            message = "La página no puede ser negativa"
        )
        int page,

        @RequestParam(defaultValue = "20")
        @Min(
            value = 1,
            message = "El tamaño debe ser como mínimo 1"
        )
        @Max(
            value = 100,
            message = "El tamaño máximo permitido es 100"
        )
        int size,

        @RequestParam(required = false)
        @Size(
            max = 100,
            message = "La búsqueda no puede superar "
                + "100 caracteres"
        )
        String search,

        @RequestParam(required = false)
        @Positive(
            message = "El identificador del usuario "
                + "debe ser positivo"
        )
        Long userId,

        @RequestParam(required = false)
        @Positive(
            message = "El identificador del ambiente "
                + "debe ser positivo"
        )
        Long roomId,

        @RequestParam(required = false)
        Boolean active,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        Instant borrowedFrom,

        @RequestParam(required = false)
        @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME
        )
        Instant borrowedTo
    ) {
        Page<AdminLoanQueryResult> result =
            loanQueryService.searchLoans(
                search,
                userId,
                roomId,
                active,
                borrowedFrom,
                borrowedTo,
                PageRequest.of(
                    page,
                    size,
                    Sort.by(
                        Sort.Order.desc("borrowedAt"),
                        Sort.Order.desc("id")
                    )
                )
            );

        return ResponseEntity.ok(
            PagedResponse.from(
                result,
                AdminLoanResponse::from
            )
        );
    }
}
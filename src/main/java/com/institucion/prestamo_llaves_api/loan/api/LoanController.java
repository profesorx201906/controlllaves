package com.institucion.prestamo_llaves_api.loan.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.institucion.prestamo_llaves_api.loan.api.dto.UserLoanResponse;
import com.institucion.prestamo_llaves_api.loan.application.LoanQueryApplicationService;
import com.institucion.prestamo_llaves_api.loan.application.LoanQueryResult;
import com.institucion.prestamo_llaves_api.shared.web.PagedResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUserIdResolver;
import com.institucion.prestamo_llaves_api.loan.api.dto.CreateLoanRequest;
import com.institucion.prestamo_llaves_api.loan.api.dto.LoanResponse;
import com.institucion.prestamo_llaves_api.loan.application.LoanApplicationService;
import com.institucion.prestamo_llaves_api.loan.application.LoanCreatedResult;
import com.institucion.prestamo_llaves_api.loan.application.LoanReturnedResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * Endpoints relacionados con préstamos y devoluciones.
 */
@Validated
@RestController
@RequestMapping("/api/v1/loans")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'USUARIO')")
public class LoanController {

    private final LoanApplicationService loanService;
    private final AuthenticatedUserIdResolver userIdResolver;
    private final LoanQueryApplicationService loanQueryService;

    public LoanController(
            LoanApplicationService loanService,
            LoanQueryApplicationService loanQueryService,
            AuthenticatedUserIdResolver userIdResolver) {
        this.loanService = loanService;
        this.loanQueryService = loanQueryService;
        this.userIdResolver = userIdResolver;
    }

    /**
     * Solicita una llave disponible.
     */
    @PostMapping
    public ResponseEntity<LoanResponse> requestLoan(

            @AuthenticationPrincipal Jwt jwt,

            @Valid @RequestBody CreateLoanRequest request) {
        Long userId = userIdResolver.resolve(jwt);

        LoanCreatedResult result = loanService.requestLoan(
                request.roomKeyId(),
                userId);

        URI location = URI.create(
                "/api/v1/loans/"
                        + result.loanId());

        return ResponseEntity
                .created(location)
                .body(LoanResponse.from(result));
    }

    /**
     * Devuelve la llave asociada al préstamo.
     */
    @PostMapping("/{loanId}/return")
    public ResponseEntity<LoanResponse> returnLoan(

            @PathVariable @Positive(message = "El identificador del préstamo "
                    + "debe ser positivo") Long loanId,

            @AuthenticationPrincipal Jwt jwt) {
        Long userId = userIdResolver.resolve(jwt);

        LoanReturnedResult result = loanService.returnLoan(
                loanId,
                userId);

        return ResponseEntity.ok(
                LoanResponse.from(result));
    }

    /**
     * Consulta los préstamos pertenecientes
     * al usuario autenticado.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<UserLoanResponse>> searchMyLoans(

            @AuthenticationPrincipal Jwt jwt,

            @RequestParam(defaultValue = "0") @Min(value = 0, message = "La página no puede ser negativa") int page,

            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño debe ser como mínimo 1") @Max(value = 100, message = "El tamaño máximo permitido es 100") int size,

            /**
             * null = todos
             * true = activos
             * false = finalizados
             */
            @RequestParam(required = false) Boolean active) {
        Long userId = userIdResolver.resolve(jwt);

        Page<LoanQueryResult> result = loanQueryService.searchUserLoans(
                userId,
                active,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Order.desc("borrowedAt"),
                                Sort.Order.desc("id"))));

        return ResponseEntity.ok(
                PagedResponse.from(
                        result,
                        UserLoanResponse::from));
    }

    /**
     * Consulta un préstamo específico del usuario autenticado.
     */
    @GetMapping("/{loanId}")
    public ResponseEntity<UserLoanResponse> getMyLoan(

            @PathVariable @Positive(message = "El identificador del préstamo "
                    + "debe ser positivo") Long loanId,

            @AuthenticationPrincipal Jwt jwt) {
        Long userId = userIdResolver.resolve(jwt);

        LoanQueryResult result = loanQueryService.getUserLoan(
                loanId,
                userId);

        return ResponseEntity.ok(
                UserLoanResponse.from(result));
    }
}
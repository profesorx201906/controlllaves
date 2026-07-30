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
@PreAuthorize(
    "hasAnyRole('ADMINISTRADOR', 'USUARIO')"
)
public class LoanController {

    private final LoanApplicationService loanService;
    private final AuthenticatedUserIdResolver userIdResolver;

    public LoanController(
            LoanApplicationService loanService,
            AuthenticatedUserIdResolver userIdResolver
    ) {
        this.loanService = loanService;
        this.userIdResolver = userIdResolver;
    }

    /**
     * Solicita una llave disponible.
     */
    @PostMapping
    public ResponseEntity<LoanResponse> requestLoan(

            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            CreateLoanRequest request
    ) {
        Long userId =
            userIdResolver.resolve(jwt);

        LoanCreatedResult result =
            loanService.requestLoan(
                request.roomKeyId(),
                userId
            );

        URI location = URI.create(
            "/api/v1/loans/"
                + result.loanId()
        );

        return ResponseEntity
            .created(location)
            .body(LoanResponse.from(result));
    }

    /**
     * Devuelve la llave asociada al préstamo.
     */
    @PostMapping("/{loanId}/return")
    public ResponseEntity<LoanResponse> returnLoan(

            @PathVariable
            @Positive(
                message = "El identificador del préstamo "
                    + "debe ser positivo"
            )
            Long loanId,

            @AuthenticationPrincipal
            Jwt jwt
    ) {
        Long userId =
            userIdResolver.resolve(jwt);

        LoanReturnedResult result =
            loanService.returnLoan(
                loanId,
                userId
            );

        return ResponseEntity.ok(
            LoanResponse.from(result)
        );
    }
}
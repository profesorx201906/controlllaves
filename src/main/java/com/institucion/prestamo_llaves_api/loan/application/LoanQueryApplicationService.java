package com.institucion.prestamo_llaves_api.loan.application;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.incident.infrastructure.persistence.IncidentRepository;
import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;
import com.institucion.prestamo_llaves_api.loan.infrastructure.persistence.LoanRepository;
import com.institucion.prestamo_llaves_api.shared.exception.InvalidRequestException;
import com.institucion.prestamo_llaves_api.shared.exception.ResourceNotFoundException;

/**
 * Consultas relacionadas con los préstamos
 * del usuario autenticado.
 */
@Service
public class LoanQueryApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LoanRepository loanRepository;
    private final IncidentRepository incidentRepository;

    public LoanQueryApplicationService(
            LoanRepository loanRepository,
            IncidentRepository incidentRepository
    ) {
        this.loanRepository = loanRepository;
        this.incidentRepository = incidentRepository;
    }

    /**
     * Consulta el historial del usuario autenticado.
     */
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'USUARIO')"
    )
    @Transactional(readOnly = true)
    public Page<LoanQueryResult> searchUserLoans(
            Long userId,
            Boolean active,
            Pageable pageable
    ) {
        validateIdentifier(userId, "userId");
        validatePageable(pageable);

        Page<Loan> loans =
            loanRepository.searchUserLoans(
                userId,
                active,
                pageable
            );

        List<Long> loanIds = loans
            .getContent()
            .stream()
            .map(Loan::getId)
            .toList();

        Set<Long> loansWithOpenIncident =
            findLoansWithOpenIncident(loanIds);

        return loans.map(loan ->
            LoanQueryResult.from(
                loan,
                loansWithOpenIncident.contains(
                    loan.getId()
                )
            )
        );
    }

    /**
     * Consulta un préstamo solo cuando pertenece
     * al usuario autenticado.
     */
    @PreAuthorize(
        "hasAnyRole('ADMINISTRADOR', 'USUARIO')"
    )
    @Transactional(readOnly = true)
    public LoanQueryResult getUserLoan(
            Long loanId,
            Long userId
    ) {
        validateIdentifier(loanId, "loanId");
        validateIdentifier(userId, "userId");

        Loan loan = loanRepository
            .findOwnedLoanById(
                loanId,
                userId
            )
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Préstamo",
                    loanId
                )
            );

        boolean openIncident =
            incidentRepository
                .existsByLoan_IdAndResolvedAtIsNull(
                    loanId
                );

        return LoanQueryResult.from(
            loan,
            openIncident
        );
    }

    private Set<Long> findLoansWithOpenIncident(
            List<Long> loanIds
    ) {
        if (loanIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(
            incidentRepository
                .findOpenIncidentLoanIds(
                    loanIds
                )
        );
    }

    private static void validatePageable(
            Pageable pageable
    ) {
        if (pageable == null
                || pageable.isUnpaged()) {

            throw new InvalidRequestException(
                "PAGINATION_REQUIRED",
                "La consulta debe indicar una paginación válida"
            );
        }

        if (pageable.getPageNumber() < 0) {
            throw new InvalidRequestException(
                "INVALID_PAGE_NUMBER",
                "La página no puede ser negativa"
            );
        }

        if (pageable.getPageSize() < 1
                || pageable.getPageSize()
                    > MAX_PAGE_SIZE) {

            throw new InvalidRequestException(
                "INVALID_PAGE_SIZE",
                "El tamaño de página debe estar entre 1 y "
                    + MAX_PAGE_SIZE
            );
        }
    }

    private static void validateIdentifier(
            Long identifier,
            String fieldName
    ) {
        if (identifier == null || identifier <= 0) {
            throw new InvalidRequestException(
                "INVALID_IDENTIFIER",
                fieldName
                    + " debe ser un identificador positivo"
            );
        }
    }
}
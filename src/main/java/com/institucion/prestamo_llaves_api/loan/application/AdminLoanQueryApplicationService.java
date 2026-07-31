package com.institucion.prestamo_llaves_api.loan.application;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

/**
 * Consultas administrativas globales de préstamos.
 */
@Service
public class AdminLoanQueryApplicationService {

    private static final int MAX_SEARCH_LENGTH = 100;
    private static final int MAX_PAGE_SIZE = 100;

    private final LoanRepository loanRepository;
    private final IncidentRepository incidentRepository;

    public AdminLoanQueryApplicationService(
            LoanRepository loanRepository,
            IncidentRepository incidentRepository
    ) {
        this.loanRepository = loanRepository;
        this.incidentRepository = incidentRepository;
    }

    /**
     * Consulta préstamos de todos los usuarios.
     */
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional(readOnly = true)
    public Page<AdminLoanQueryResult> searchLoans(
            String search,
            Long userId,
            Long roomId,
            Boolean active,
            Instant borrowedFrom,
            Instant borrowedTo,
            Pageable pageable
    ) {
        validateOptionalIdentifier(
            userId,
            "userId"
        );

        validateOptionalIdentifier(
            roomId,
            "roomId"
        );

        validateDateRange(
            borrowedFrom,
            borrowedTo
        );

        validatePageable(pageable);

        Page<Loan> loans =
            loanRepository.searchAdminLoans(
                normalizeSearch(search),
                userId,
                roomId,
                active,
                borrowedFrom,
                borrowedTo,
                pageable
            );

        List<Long> loanIds = loans
            .getContent()
            .stream()
            .map(Loan::getId)
            .toList();

        Set<Long> loansWithOpenIncident =
            findOpenIncidentLoanIds(loanIds);

        return loans.map(loan ->
            AdminLoanQueryResult.from(
                loan,
                loansWithOpenIncident.contains(
                    loan.getId()
                )
            )
        );
    }

    private Set<Long> findOpenIncidentLoanIds(
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

    private static String normalizeSearch(
            String search
    ) {
        if (search == null || search.isBlank()) {
            return null;
        }

        String normalizedSearch = search
            .trim()
            .toLowerCase(Locale.ROOT);

        if (normalizedSearch.length()
                > MAX_SEARCH_LENGTH) {

            throw new InvalidRequestException(
                "SEARCH_TOO_LONG",
                "La búsqueda no puede superar "
                    + MAX_SEARCH_LENGTH
                    + " caracteres"
            );
        }

        return normalizedSearch;
    }

    private static void validateDateRange(
            Instant borrowedFrom,
            Instant borrowedTo
    ) {
        if (borrowedFrom != null
                && borrowedTo != null
                && borrowedFrom.isAfter(borrowedTo)) {

            throw new InvalidRequestException(
                "INVALID_DATE_RANGE",
                "La fecha inicial no puede ser posterior "
                    + "a la fecha final"
            );
        }
    }

    private static void validateOptionalIdentifier(
            Long identifier,
            String fieldName
    ) {
        if (identifier != null && identifier <= 0) {
            throw new InvalidRequestException(
                "INVALID_IDENTIFIER",
                fieldName
                    + " debe ser un identificador positivo"
            );
        }
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
}
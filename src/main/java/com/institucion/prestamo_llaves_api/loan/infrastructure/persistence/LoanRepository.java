package com.institucion.prestamo_llaves_api.loan.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.institucion.prestamo_llaves_api.loan.domain.model.Loan;

/**
 * Acceso al historial de préstamos y devoluciones.
 */
public interface LoanRepository
        extends JpaRepository<Loan, Long> {

    /**
     * Busca el préstamo activo de una llave.
     *
     * Por la restricción única de MariaDB, como máximo debe
     * existir un resultado.
     */
    @Query("""
            SELECT loan
            FROM Loan loan
            WHERE loan.roomKey.id = :keyId
              AND loan.returnedAt IS NULL
              AND loan.activeSlot IS NOT NULL
            """)
    Optional<Loan> findActiveByRoomKeyId(
            @Param("keyId") Long keyId);

    /**
     * Obtiene únicamente el identificador de la llave asociada
     * a un préstamo.
     *
     * Se usa antes de bloquear la llave en operaciones que deben
     * coordinarse con la devolución.
     */
    @Query("""
            SELECT loan.roomKey.id
            FROM Loan loan
            WHERE loan.id = :loanId
            """)
    Optional<Long> findRoomKeyIdByLoanId(
            @Param("loanId") Long loanId);

    /**
     * Busca un préstamo por su identificador, únicamente
     * cuando todavía está activo.
     */
    @Query("""
            SELECT loan
            FROM Loan loan
            WHERE loan.id = :loanId
              AND loan.returnedAt IS NULL
              AND loan.activeSlot IS NOT NULL
            """)
    Optional<Loan> findActiveById(
            @Param("loanId") Long loanId);

    /**
     * Recupera todos los préstamos activos de un usuario.
     *
     * Actualmente un usuario puede tener más de una llave
     * prestada simultáneamente porque no se definió una
     * restricción funcional que lo impida.
     */
    @Query("""
            SELECT loan
            FROM Loan loan
            WHERE loan.user.id = :userId
              AND loan.returnedAt IS NULL
              AND loan.activeSlot IS NOT NULL
            ORDER BY loan.borrowedAt DESC
            """)
    List<Loan> findAllActiveByUserId(
            @Param("userId") Long userId);

    /**
     * Historial paginado de préstamos de un usuario.
     *
     * No devolvemos todo el historial en una lista para evitar
     * consultas sin límite cuando aumente el volumen de datos.
     */
    Page<Loan> findAllByUser_IdOrderByBorrowedAtDesc(
            Long userId,
            Pageable pageable);
}
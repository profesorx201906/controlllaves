package com.institucion.prestamo_llaves_api.auth.application;


import org.springframework.stereotype.Service;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUser;

/**
 * Coordina el cambio de contraseña y la emisión
 * del nuevo token de acceso.
 */
@Service
public class PasswordChangeApplicationService {

    private final PasswordChangeTransactionService
        transactionService;

    private final JwtTokenService jwtTokenService;

    public PasswordChangeApplicationService(
            PasswordChangeTransactionService
                transactionService,
            JwtTokenService jwtTokenService
    ) {
        this.transactionService = transactionService;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult changePassword(
            Long userId,
            String currentPassword,
            String newPassword
    ) {
        /*
         * Cuando esta llamada retorna, la transacción
         * de actualización ya terminó.
         */
        AuthenticatedUser user =
            transactionService.changePassword(
                userId,
                currentPassword,
                newPassword
            );

        IssuedToken token =
            jwtTokenService.issueToken(user);

        return new LoginResult(
            token.value(),
            "Bearer",
            token.expiresAt(),
            user.id(),
            user.fullName(),
            user.email(),
            user.role(),
            user.mustChangePassword()
        );
    }
}
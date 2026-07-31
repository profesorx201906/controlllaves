package com.institucion.prestamo_llaves_api.auth.infrastructure.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserRepository;
import com.institucion.prestamo_llaves_api.user.infrastructure.persistence.UserSecurityStateView;

/**
 * Comprueba que:
 *
 * - el usuario todavía exista;
 * - la cuenta permanezca habilitada;
 * - el JWT tenga la versión de seguridad vigente.
 */
@Component
public class JwtAccountStateValidator
        implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_TOKEN =
        new OAuth2Error(
            "invalid_token",
            "El token ya no es válido",
            null
        );

    private final UserRepository userRepository;

    public JwtAccountStateValidator(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OAuth2TokenValidatorResult validate(
            Jwt jwt
    ) {
        Long userId = extractUserId(jwt);

        if (userId == null) {
            return invalid();
        }

        Long tokenVersion =
            extractTokenVersion(jwt);

        if (tokenVersion == null
                || tokenVersion < 0) {

            return invalid();
        }

        UserSecurityStateView userState =
            userRepository
                .findSecurityStateById(userId)
                .orElse(null);

        if (userState == null) {
            return invalid();
        }

        if (!Boolean.TRUE.equals(
                userState.getEnabled()
        )) {
            return invalid();
        }

        if (!tokenVersion.equals(
                userState.getTokenVersion()
        )) {
            return invalid();
        }

        return OAuth2TokenValidatorResult.success();
    }

    private static Long extractUserId(Jwt jwt) {
        if (jwt == null
                || jwt.getSubject() == null
                || jwt.getSubject().isBlank()) {

            return null;
        }

        try {
            long userId = Long.parseLong(
                jwt.getSubject()
            );

            return userId > 0
                ? userId
                : null;

        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long extractTokenVersion(
            Jwt jwt
    ) {
        Object claim =
            jwt.getClaim("token_version");

        if (!(claim instanceof Number number)) {
            return null;
        }

        return number.longValue();
    }

    private static OAuth2TokenValidatorResult
            invalid() {

        return OAuth2TokenValidatorResult.failure(
            INVALID_TOKEN
        );
    }
}
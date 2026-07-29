package com.institucion.prestamo_llaves_api.auth.application;


import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.AuthenticatedUser;
import com.institucion.prestamo_llaves_api.auth.infrastructure.security.JwtProperties;

/**
 * Genera tokens de acceso firmados.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issueToken(
            AuthenticatedUser user
    ) {
        validateTokenTtl();

        Instant issuedAt = clock.instant();
        Instant expiresAt =
            issuedAt.plus(properties.accessTokenTtl());

        List<String> authorities =
            determineAuthorities(user);

        JwsHeader headers = JwsHeader
            .with(MacAlgorithm.HS256)
            .type("JWT")
            .build();

        JwtClaimsSet claims = JwtClaimsSet
            .builder()
            .issuer(properties.issuer())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)

            /*
             * El subject contiene el identificador estable
             * del usuario, no el correo modificable.
             */
            .subject(user.id().toString())

            .claim("email", user.email())
            .claim("name", user.fullName())
            .claim("role", user.role().name())
            .claim("authorities", authorities)
            .claim(
                "must_change_password",
                user.mustChangePassword()
            )
            .build();

        String token = jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    headers,
                    claims
                )
            )
            .getTokenValue();

        return new IssuedToken(
            token,
            expiresAt
        );
    }

    private static List<String> determineAuthorities(
            AuthenticatedUser user
    ) {
        if (user.mustChangePassword()) {
            return List.of(
                "PASSWORD_CHANGE_REQUIRED"
            );
        }

        return List.of(
            user.role().name()
        );
    }

    private void validateTokenTtl() {
        if (properties.accessTokenTtl().isZero()
                || properties.accessTokenTtl().isNegative()) {

            throw new IllegalStateException(
                "JWT_ACCESS_TOKEN_TTL debe ser positivo"
            );
        }
    }
}
package com.institucion.prestamo_llaves_api.auth.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.institucion.prestamo_llaves_api.shared.web.ApiError;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Devuelve errores 401 usando el formato JSON de la API.
 */
@Component
public class JsonAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final BearerTokenAuthenticationEntryPoint
        delegate =
            new BearerTokenAuthenticationEntryPoint();

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public JsonAuthenticationEntryPoint(
            JsonMapper jsonMapper,
            Clock clock
    ) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        /*
         * Mantiene el encabezado WWW-Authenticate
         * requerido para Bearer Tokens.
         */
        delegate.commence(
            request,
            response,
            exception
        );

        response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
        );

        response.setContentType(
            MediaType.APPLICATION_JSON_VALUE
        );

        ApiError error = new ApiError(
            clock.instant(),
            HttpServletResponse.SC_UNAUTHORIZED,
            "UNAUTHORIZED",
            "Se requiere un token de acceso válido",
            request.getRequestURI()
        );

        jsonMapper.writeValue(
            response.getOutputStream(),
            error
        );
    }
}
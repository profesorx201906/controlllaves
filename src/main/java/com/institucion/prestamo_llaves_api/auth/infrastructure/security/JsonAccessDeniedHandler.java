package com.institucion.prestamo_llaves_api.auth.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.institucion.prestamo_llaves_api.shared.web.ApiError;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Devuelve errores 403 usando el formato JSON de la API.
 */
@Component
public class JsonAccessDeniedHandler
        implements AccessDeniedHandler {

    private final BearerTokenAccessDeniedHandler
        delegate =
            new BearerTokenAccessDeniedHandler();

    private final JsonMapper jsonMapper;
    private final Clock clock;

    public JsonAccessDeniedHandler(
            JsonMapper jsonMapper,
            Clock clock
    ) {
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException, ServletException {

        delegate.handle(
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
            HttpServletResponse.SC_FORBIDDEN,
            "ACCESS_DENIED",
            "No tiene permisos para realizar esta operación",
            request.getRequestURI()
        );

        jsonMapper.writeValue(
            response.getOutputStream(),
            error
        );
    }
}
package com.institucion.prestamo_llaves_api.auth.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(
    CorsProperties.class
)
public class CorsSecurityConfig {

    @Bean
    UrlBasedCorsConfigurationSource
            corsConfigurationSource(
                CorsProperties properties
            ) {

        CorsConfiguration configuration =
            new CorsConfiguration();

        /*
         * No utilizar "*" en producción.
         */
        configuration.setAllowedOrigins(
            properties.allowedOrigins()
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PATCH",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
            )
        );

        configuration.setExposedHeaders(
            List.of(
                HttpHeaders.LOCATION
            )
        );

        /*
         * La API usa JWT en Authorization y no cookies
         * de sesión.
         */
        configuration.setAllowCredentials(false);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }
}
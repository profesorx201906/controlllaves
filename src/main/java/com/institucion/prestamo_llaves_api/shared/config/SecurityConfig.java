package com.institucion.prestamo_llaves_api.shared.config;


import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración principal de autenticación y autorización.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
            new DaoAuthenticationProvider(
                userDetailsService
            );

        provider.setPasswordEncoder(
            passwordEncoder
        );

        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(
            List.of(authenticationProvider)
        );
    }

    @Bean
    JwtAuthenticationConverter
            jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
            new JwtGrantedAuthoritiesConverter();

        /*
         * Lee la lista personalizada:
         *
         * "authorities": ["ADMINISTRADOR"]
         */
        authoritiesConverter.setAuthoritiesClaimName(
            "authorities"
        );

        authoritiesConverter.setAuthorityPrefix(
            "ROLE_"
        );

        JwtAuthenticationConverter authenticationConverter =
            new JwtAuthenticationConverter();

        authenticationConverter
            .setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
            );

        return authenticationConverter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationConverter jwtConverter
    ) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .httpBasic(httpBasic ->
                httpBasic.disable()
            )

            .formLogin(formLogin ->
                formLogin.disable()
            )

            .logout(logout ->
                logout.disable()
            )

            .requestCache(requestCache ->
                requestCache.disable()
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authenticationProvider(
                authenticationProvider
            )

            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/login"
                ).permitAll()

                /*
                 * Se implementará en la siguiente parte.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/change-password"
                ).authenticated()

                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()

                .requestMatchers(
                    "/actuator/**"
                ).hasRole("ADMINISTRADOR")

                /*
                 * Los usuarios que aún deben cambiar su
                 * contraseña no poseen estos roles efectivos.
                 */
                .anyRequest()
                    .hasAnyRole(
                        "ADMINISTRADOR",
                        "USUARIO"
                    )
            )

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(
                        jwtConverter
                    )
                )
            );

        return http.build();
    }
}

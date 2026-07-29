package com.institucion.prestamo_llaves_api.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración temporal de seguridad.
 *
 * Será reemplazada cuando implementemos la autenticación JWT.
 */
@Configuration
public class InitialSecurityConfig {

    @Bean
    SecurityFilterChain initialSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // La futura API utilizará JWT y no sesiones del servidor.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // La autenticación Basic generada por defecto no será utilizada.
            .httpBasic(httpBasic -> httpBasic.disable())

            // Desactiva el formulario HTML de inicio de sesión.
            .formLogin(formLogin -> formLogin.disable())

            // CSRF se desactiva porque la API será stateless y usará JWT.
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(authorize -> authorize
                // Endpoint público usado para verificar la aplicación.
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**"
                ).permitAll()

                // No exponer todavía ningún endpoint funcional.
                .anyRequest().denyAll()
            );

        return http.build();
    }
}
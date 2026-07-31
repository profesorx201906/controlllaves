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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.institucion.prestamo_llaves_api.auth.infrastructure.security.JsonAccessDeniedHandler;
import com.institucion.prestamo_llaves_api.auth.infrastructure.security.JsonAuthenticationEntryPoint;

/**
 * Configuración central de seguridad de la API.
 *
 * Responsabilidades:
 *
 * - Autenticación de correo y contraseña.
 * - Validación de JWT Bearer.
 * - Autorización por roles.
 * - API sin sesiones HTTP.
 * - Integración de CORS con React.
 * - Respuestas JSON para errores 401 y 403.
 * - Cabeceras HTTP de seguridad.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        /**
         * Proveedor utilizado durante POST /api/v1/auth/login.
         *
         * DatabaseUserDetailsService carga el usuario desde MariaDB
         * y PasswordEncoder comprueba la contraseña recibida contra
         * el hash almacenado.
         */
        @Bean
        AuthenticationProvider authenticationProvider(
                        UserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                                userDetailsService);

                provider.setPasswordEncoder(
                                passwordEncoder);

                return provider;
        }

        /**
         * AuthenticationManager utilizado por
         * AuthenticationApplicationService.
         */
        @Bean
        AuthenticationManager authenticationManager(
                        AuthenticationProvider authenticationProvider) {
                return new ProviderManager(
                                List.of(authenticationProvider));
        }

        /**
         * Convierte el claim personalizado "authorities"
         * del JWT en autoridades reconocidas por Spring Security.
         *
         * Ejemplo del token:
         *
         * "authorities": ["ADMINISTRADOR"]
         *
         * Autoridad producida:
         *
         * ROLE_ADMINISTRADOR
         */
        @Bean
        JwtAuthenticationConverter jwtAuthenticationConverter() {

                JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

                authoritiesConverter.setAuthoritiesClaimName(
                                "authorities");

                authoritiesConverter.setAuthorityPrefix(
                                "ROLE_");

                JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();

                authenticationConverter
                                .setJwtGrantedAuthoritiesConverter(
                                                authoritiesConverter);

                /*
                 * El nombre de la autenticación será el claim sub,
                 * que contiene el identificador del usuario.
                 */
                authenticationConverter
                                .setPrincipalClaimName("sub");

                return authenticationConverter;
        }

        /**
         * Cadena principal de filtros de Spring Security.
         */
        @Bean
        SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        AuthenticationProvider authenticationProvider,
                        JwtAuthenticationConverter jwtAuthenticationConverter,
                        UrlBasedCorsConfigurationSource corsConfigurationSource,
                        JsonAuthenticationEntryPoint authenticationEntryPoint,
                        JsonAccessDeniedHandler accessDeniedHandler) throws Exception {

                http
                                /*
                                 * La API utiliza JWT en Authorization.
                                 *
                                 * No usa cookies de sesión para autenticación,
                                 * por lo que CSRF no aplica al mecanismo actual.
                                 */
                                .csrf(csrf -> csrf.disable())

                                /*
                                 * Usa la configuración definida en
                                 * CorsSecurityConfig.
                                 */
                                .cors(cors -> cors.configurationSource(
                                                corsConfigurationSource))

                                /*
                                 * No se utiliza autenticación HTTP Basic.
                                 */
                                .httpBasic(httpBasic -> httpBasic.disable())

                                /*
                                 * No se utiliza formulario HTML de inicio
                                 * de sesión.
                                 */
                                .formLogin(formLogin -> formLogin.disable())

                                /*
                                 * No existe logout basado en sesión.
                                 *
                                 * La revocación de JWT se gestiona mediante
                                 * token_version.
                                 */
                                .logout(logout -> logout.disable())

                                /*
                                 * Impide que Spring guarde solicitudes para
                                 * redirecciones posteriores.
                                 */
                                .requestCache(requestCache -> requestCache.disable())

                                /*
                                 * La API no crea ni utiliza HttpSession
                                 * para conservar autenticación.
                                 */
                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                /*
                                 * Proveedor para autenticación con correo
                                 * y contraseña en el endpoint de login.
                                 */
                                .authenticationProvider(
                                                authenticationProvider)

                                /*
                                 * Respuestas JSON uniformes para errores
                                 * producidos dentro de los filtros.
                                 */
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(
                                                                authenticationEntryPoint)
                                                .accessDeniedHandler(
                                                                accessDeniedHandler))

                                /*
                                 * Reglas de autorización HTTP.
                                 *
                                 * El orden es importante: las reglas más
                                 * específicas deben aparecer primero.
                                 */
                                .authorizeHttpRequests(authorize -> authorize

                                                /*
                                                 * Las solicitudes preflight del navegador
                                                 * no necesitan JWT.
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.OPTIONS,
                                                                "/**")
                                                .permitAll()

                                                /*
                                                 * Inicio de sesión público.
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/auth/login")
                                                .permitAll()

                                                /*
                                                 * Requiere un JWT válido, pero permite
                                                 * tokens que todavía solo tienen:
                                                 *
                                                 * ROLE_PASSWORD_CHANGE_REQUIRED
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/auth/change-password")
                                                .authenticated()

                                                /*
                                                 * Health check público.
                                                 */
                                                .requestMatchers(
                                                                "/actuator/health",
                                                                "/actuator/health/**")
                                                .permitAll()

                                                /*
                                                 * Los demás endpoints de Actuator
                                                 * requieren administrador.
                                                 */
                                                .requestMatchers(
                                                                "/actuator/**")
                                                .hasRole("ADMINISTRADOR")

                                                /*
                                                 * Usuarios, ambientes, préstamos e
                                                 * incidencias administrativas.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/admin/**")
                                                .hasRole("ADMINISTRADOR")

                                                /*
                                                 * Solicitud, devolución, consulta y
                                                 * reporte de incidencias de préstamos.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/loans/**")
                                                .hasAnyRole(
                                                                "ADMINISTRADOR",
                                                                "USUARIO")

                                                /*
                                                 * Catálogo de ambientes y llaves.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/rooms/**")
                                                .hasAnyRole(
                                                                "ADMINISTRADOR",
                                                                "USUARIO")

                                                /*
                                                 * Cualquier endpoint no declarado
                                                 * requiere un rol funcional.
                                                 *
                                                 * Un token con cambio de contraseña
                                                 * pendiente no cumple esta condición.
                                                 */
                                                .anyRequest()
                                                .hasAnyRole(
                                                                "ADMINISTRADOR",
                                                                "USUARIO"))

                                .headers(headers -> {

                                        /*
                                         * Impide mostrar la API dentro de un iframe.
                                         */
                                        headers.frameOptions(frameOptions -> frameOptions.deny());

                                        /*
                                         * La API no entrega contenido HTML ni recursos web.
                                         */
                                        headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                                                        "default-src 'none'; "
                                                                        + "frame-ancestors 'none'; "
                                                                        + "base-uri 'none'; "
                                                                        + "form-action 'none'"));

                                        headers.referrerPolicy(referrerPolicy -> referrerPolicy.policy(
                                                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));

                                        /*
                                         * En Spring Security reciente se utiliza
                                         * permissionsPolicyHeader.
                                         */
                                        headers.permissionsPolicyHeader(permissionsPolicy -> permissionsPolicy.policy(
                                                        "camera=(), "
                                                                        + "microphone=(), "
                                                                        + "geolocation=(), "
                                                                        + "payment=(), "
                                                                        + "usb=()"));

                                        /*
                                         * Esta configuración debe ejecutarse directamente
                                         * sobre headers, no sobre permissionsPolicy.
                                         */
                                        headers.httpStrictTransportSecurity(hsts -> hsts
                                                        .includeSubDomains(true)
                                                        .preload(true)
                                                        .maxAgeInSeconds(31536000));
                                })

                                /*
                                 * Configura la aplicación como Resource Server.
                                 *
                                 * JwtDecoder:
                                 * - verifica firma;
                                 * - valida issuer;
                                 * - valida expiración;
                                 * - valida token_version;
                                 * - verifica que el usuario siga habilitado.
                                 */
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .authenticationEntryPoint(
                                                                authenticationEntryPoint)
                                                .accessDeniedHandler(
                                                                accessDeniedHandler)
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter)));

                return http.build();
        }
}
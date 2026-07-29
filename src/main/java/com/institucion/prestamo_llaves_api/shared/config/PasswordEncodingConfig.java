package com.institucion.prestamo_llaves_api.shared.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración centralizada del cifrado de contraseñas.
 */
@Configuration
public class PasswordEncodingConfig {

    /**
     * Crea un DelegatingPasswordEncoder.
     *
     * El resultado almacenado incluye el identificador
     * del algoritmo, por ejemplo:
     *
     * {bcrypt}$2a$...
     *
     * Esto permitirá cambiar el algoritmo en el futuro
     * manteniendo compatibilidad con hashes anteriores.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    }
}
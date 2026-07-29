package com.institucion.prestamo_llaves_api.bootstrap.infrastructure.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra la configuración del administrador inicial
 * como un bean administrado por Spring.
 */
@Configuration
@EnableConfigurationProperties(
    BootstrapAdminProperties.class
)
public class BootstrapAdminConfig {
}

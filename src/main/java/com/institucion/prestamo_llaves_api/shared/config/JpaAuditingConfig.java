package com.institucion.prestamo_llaves_api.shared.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita el llenado automático de las fechas de creación
 * y última modificación de las entidades JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
package com.institucion.prestamo_llaves_api.shared.config;


import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración centralizada del reloj de la aplicación.
 */
@Configuration
public class TimeConfig {

    /**
     * Todas las fechas internas se calculan en UTC.
     *
     * Durante las pruebas podremos reemplazar este reloj
     * por uno fijo.
     */
    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
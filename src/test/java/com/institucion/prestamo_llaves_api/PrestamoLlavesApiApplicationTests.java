package com.institucion.prestamo_llaves_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica que el contexto completo de Spring pueda iniciar.
 */
@SpringBootTest
@ActiveProfiles("test")
class PrestamoLlavesApiApplicationTests {

    @Test
    void contextLoads() {
        // La prueba es correcta si el contexto inicia sin errores.
    }
}
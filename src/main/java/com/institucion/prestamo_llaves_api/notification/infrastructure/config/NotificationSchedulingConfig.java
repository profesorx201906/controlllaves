package com.institucion.prestamo_llaves_api.notification.infrastructure.config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
/**
 * Configuración del procesador programado de notificaciones.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationSchedulingConfig {

    /**
     * Un único hilo evita procesar dos lotes simultáneamente
     * dentro de esta misma instancia de la aplicación.
     */
    @Bean
    TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler =
            new ThreadPoolTaskScheduler();

        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix(
            "incident-notification-"
        );

        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(20);

        return scheduler;
    }
    
}
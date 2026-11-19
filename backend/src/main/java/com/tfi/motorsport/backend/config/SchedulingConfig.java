package com.tfi.motorsport.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas programadas (@Scheduled).
 * La sincronización automática igualmente solo se ejecuta si SYNC_AUTOMATICA=true.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}


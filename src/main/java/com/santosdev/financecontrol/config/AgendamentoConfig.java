package com.santosdev.financecontrol.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Liga as tarefas periodicas (ex.: apagar contas de visitante expiradas). */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}

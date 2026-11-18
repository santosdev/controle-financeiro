package com.santosdev.financecontrol.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Permite enviar e-mails em segundo plano, sem deixar o usuario esperando o servidor SMTP responder. */
@Configuration
@EnableAsync
public class AsyncConfig {
}

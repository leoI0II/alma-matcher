package com.almamatcher.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.almamatcher.config.AlmaMatcherProperties;

@Component
public class LoggingEmailSender implements EmailSenderService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    private final AlmaMatcherProperties properties;

    public LoggingEmailSender(
        final AlmaMatcherProperties props
    ) {
        this.properties = props;
    }

    @Override
    public void sendVerification(String to, String token) {
        log.info("Verification link for {}: {}/api/auth/verify?token={}",
            to, properties.emailVerification().baseUrl(), token
        );
    }
    
}

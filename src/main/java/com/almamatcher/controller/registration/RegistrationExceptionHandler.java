package com.almamatcher.controller.registration;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.almamatcher.model.exceptions.EmailAlreadyInUseException;
import com.almamatcher.model.exceptions.EmailDomainNotAllowedException;
import com.almamatcher.model.exceptions.ExpiredVerificationTokenException;
import com.almamatcher.model.exceptions.InvalidVerificationTokenException;
import com.almamatcher.model.exceptions.NotAdultEnoughException;
import com.almamatcher.model.exceptions.UsernameAlreadyTakenException;

@RestControllerAdvice
public class RegistrationExceptionHandler {
    @ExceptionHandler({
        UsernameAlreadyTakenException.class,
        EmailAlreadyInUseException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleConflict(final RuntimeException exception) {
        return body(exception.getMessage());
    }
    
    @ExceptionHandler({
        NotAdultEnoughException.class,
        EmailDomainNotAllowedException.class,
        ExpiredVerificationTokenException.class,
        InvalidVerificationTokenException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(final RuntimeException exception) {
        return body(exception.getMessage());
    }

    private Map<String, Object> body(final String msg) {
        return Map.of("message", msg, "timestamp", Instant.now().toString());
    }
}

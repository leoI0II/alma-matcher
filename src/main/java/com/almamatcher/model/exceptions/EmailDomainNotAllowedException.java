package com.almamatcher.model.exceptions;

public class EmailDomainNotAllowedException extends RuntimeException {
    public EmailDomainNotAllowedException(String email) {
        super(email + " is not allowed. Only @studio.unibo.it / @unibo.it.");
    }
}

package com.almamatcher.model.exceptions;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException() {
        super("This email is already in use. Sign in!");
    }
}

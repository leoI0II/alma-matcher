package com.almamatcher.model.exceptions;

public class UsernameAlreadyTakenException extends RuntimeException {
    
    public UsernameAlreadyTakenException(String username) {
        super(username + " already exists. Try another one.");
    }
}

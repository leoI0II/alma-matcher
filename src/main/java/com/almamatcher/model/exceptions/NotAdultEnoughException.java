package com.almamatcher.model.exceptions;

public class NotAdultEnoughException extends RuntimeException {
    public NotAdultEnoughException() {
        super("You must be atleast 18 years old.");
    }
}

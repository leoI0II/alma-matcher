package com.almamatcher.model.exceptions;

public class EmailDomainNotAllowedException extends RuntimeException {
    public EmailDomainNotAllowedException(String domain) {
        super("Registration is restricted to UniBo students. "
              + "The domain " + domain + " is not allowed.");
    }
}

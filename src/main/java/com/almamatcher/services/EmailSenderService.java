package com.almamatcher.services;

public interface EmailSenderService {
    void sendVerification(String to, String token);
}

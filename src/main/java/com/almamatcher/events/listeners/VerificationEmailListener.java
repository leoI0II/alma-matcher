package com.almamatcher.events.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.almamatcher.events.AccountRegisteredEvent;
import com.almamatcher.services.EmailSenderService;

@Component
public class VerificationEmailListener {
    
    private final EmailSenderService emailSender;

    public VerificationEmailListener(final EmailSenderService emailSender) {
        this.emailSender = emailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountRegistered(final AccountRegisteredEvent event) {
        emailSender.sendVerification(event.email(), event.token());
    }
}

package com.almamatcher.services;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.almamatcher.config.AlmaMatcherProperties;
import com.almamatcher.model.data.Account;
import com.almamatcher.model.data.Profile;
import com.almamatcher.model.data.RegistrationRequest;
import com.almamatcher.model.exceptions.EmailAlreadyInUseException;
import com.almamatcher.model.exceptions.EmailDomainNotAllowedException;
import com.almamatcher.model.exceptions.NotAdultEnoughException;
import com.almamatcher.model.exceptions.UsernameAlreadyTakenException;
import com.almamatcher.repository.AccountRepository;
import com.almamatcher.repository.ProfileRepository;

@Service
public class RegistrationService {
    
    private final AlmaMatcherProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;
    private final ProfileRepository profileRepository;

    public RegistrationService(
        final AlmaMatcherProperties properties,
        final PasswordEncoder passwordEncoder,
        final AccountRepository accountRepository,
        final ProfileRepository profileRepository
    ) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
    }

    private boolean isAllowedByAge(final LocalDate birthDate) {
        final var age = Period.between(birthDate, LocalDate.now());
        boolean isAdultEnough = age.getYears() >= 18;
        return isAdultEnough;
    }

    private boolean alreadyExistsByEmail(final String email) {
        return accountRepository.existsByEmail(email);
    }

    private boolean alreadyExistsByUsername(final String username) {
        return accountRepository.existsByUsername(username);
    }

    private String extractDomain(final String email) {
        final int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            throw new EmailDomainNotAllowedException(email);
        }
        return email.substring(at + 1);
    }

    private boolean isAllowedDomain(final String domain) {
        return properties.emailDomains().contains(domain);
    }

    @Transactional
    public void register(final RegistrationRequest request) {
        final String email = request.email().trim().toLowerCase();
        final String domain = extractDomain(email);
        if (!isAllowedDomain(domain)) {
            throw new EmailDomainNotAllowedException(extractDomain(request.email()));
        }
        if (alreadyExistsByEmail(email)) {
            throw new EmailAlreadyInUseException();
        }
        if (!isAllowedByAge(request.birthDate())) {
            throw new NotAdultEnoughException();
        }
        if (alreadyExistsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException(request.username());
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Account account = Account.createNewAccount(
            request.email().toLowerCase(), 
            passwordHash, 
            request.username()
        );
        accountRepository.save(account);

        Profile profile = new Profile(
            request.firstName(), 
            request.lastName(), 
            request.birthDate(), 
            account
        );
        profileRepository.save(profile);
    }

}

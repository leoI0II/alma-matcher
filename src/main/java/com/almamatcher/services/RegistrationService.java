package com.almamatcher.services;

import java.time.LocalDate;
import java.time.Period;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

import jakarta.transaction.Transactional;

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

    private boolean isAllowedByAge(final RegistrationRequest request) {
        final var age = Period.between(request.birthDate(), LocalDate.now());
        boolean isAdultEnough = age.getYears() >= 18;
        return isAdultEnough;
    }

    private boolean alreadyExistsByEmail(final RegistrationRequest request) {
        return accountRepository.existsByEmail(request.email());
    }

    private boolean alreadyExistsByUsername(final RegistrationRequest request) {
        return accountRepository.existsByEmail(request.username());
    }

    private String extractDomain(final String email) {
        final int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            throw new EmailDomainNotAllowedException(email);
        }
        return email.substring(at + 1);
    }

    private boolean isAllowedDomain(final RegistrationRequest request) {
        return properties.emailDomains()
                .contains(
                    extractDomain(request.email())
                );
    }

    @Transactional
    public void register(final RegistrationRequest request) {
        if (alreadyExistsByUsername(request)) {
            throw new UsernameAlreadyTakenException(request.username());
        }
        if (alreadyExistsByEmail(request)) {
            throw new EmailAlreadyInUseException();
        }
        if (!isAllowedByAge(request)) {
            throw new NotAdultEnoughException();
        }
        if (!isAllowedDomain(request)) {
            throw new EmailDomainNotAllowedException(extractDomain(request.email()));
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

// JpaRepository<T1, T2> dici che sia l entita e il tipo della sua chiave... non capisco cosa intendi. che Account sia valore (T1), e T2 e' la chiave? in questo caso dell ultimo esempio Account sia valore e UUID e' chiave? non ho capito..
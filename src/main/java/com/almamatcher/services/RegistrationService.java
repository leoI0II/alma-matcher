package com.almamatcher.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.almamatcher.model.data.Account;
import com.almamatcher.model.data.RegistrationRequest;
import com.almamatcher.model.exceptions.UsernameAlreadyTakenException;
import com.almamatcher.repository.AccountRepository;

@Service
public class RegistrationService {
    
    private final PasswordEncoder passwordEncoder;
    private final AccountRepository accountRepository;

    public RegistrationService(
        final PasswordEncoder passwordEncoder,
        final AccountRepository accountRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.accountRepository = accountRepository;
    }

    public void register(final RegistrationRequest request) throws UsernameAlreadyTakenException {
        if (accountRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException();
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Account account = Account.createNewAccount(
            request.email().toLowerCase(), 
            passwordHash, 
            request.username()
        );
        accountRepository.save(account);
    }

}

// JpaRepository<T1, T2> dici che sia l entita e il tipo della sua chiave... non capisco cosa intendi. che Account sia valore (T1), e T2 e' la chiave? in questo caso dell ultimo esempio Account sia valore e UUID e' chiave? non ho capito..
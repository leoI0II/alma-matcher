package com.almamatcher.model.data.generator;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class TokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom RND = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RND.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }

}

package com.almamatcher.controller.registration;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.almamatcher.config.AlmaMatcherProperties;
import com.almamatcher.model.data.RegistrationRequest;
import com.almamatcher.services.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AlmaMatcherProperties properties;

    public RegistrationController(
        final RegistrationService registrationService,
        final AlmaMatcherProperties props
    ) {
        this.registrationService = registrationService;
        this.properties = props;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody final RegistrationRequest request) {
        registrationService.register(request);
    }

    @GetMapping("/verify")
    // @ResponseStatus(HttpStatus.OK)   //??
    public ResponseEntity<Void> verify(@RequestParam final String token) {
        registrationService.verify(token);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(
                    URI.create(
                        properties.emailVerification().baseUrl() + "/verified.html"
                    )
                )
                .build();
    }
    
}

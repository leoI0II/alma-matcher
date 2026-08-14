package com.almamatcher.controller.registration;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.almamatcher.model.data.RegistrationRequest;
import com.almamatcher.services.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/auth")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody final RegistrationRequest request) {
        registrationService.register(request);
    }
    
}

package com.almamatcher.model.data;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest (
    
    @Email @NotBlank
    String email,

    @NotBlank @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-z0-9_.]+$")
    String username,

    @NotBlank @Size(min = 12, max = 100)
    String password,

    @NotBlank @Size(max = 30)
    String firstName,

    @NotBlank @Size(max = 30)
    String lastName,

    @NotNull @Past
    LocalDate birthDate

) {}

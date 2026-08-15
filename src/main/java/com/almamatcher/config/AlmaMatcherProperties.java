package com.almamatcher.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "alma-matcher")
public record AlmaMatcherProperties(
    
    @NotEmpty
    List<String> emailDomains,

    @NotNull @Valid
    Username username,

    @NotNull @Valid
    EmailVerification emailVerification
){
    public record Username(
        @Min(3) int minLength,
        @Min(3) int maxLength,
        @NotBlank String pattern
    ){}
    public record EmailVerification(
        @NotNull Duration tokenValidity,
        @NotBlank String baseUrl
    ){}
}

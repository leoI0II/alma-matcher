package com.almamatcher.events;

public record AccountRegisteredEvent(
    String email,
    String token
) {}

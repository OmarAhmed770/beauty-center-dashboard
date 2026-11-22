package com.omar.beautycenter.auth;

public class RegistrationRateLimitException extends RuntimeException {

    public RegistrationRateLimitException() {
        super("Too many registration requests. Please try again later.");
    }
}

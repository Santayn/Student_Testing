package org.santayn.testing.service;

public class AuthConflictException extends RuntimeException {

    public AuthConflictException(String message) {
        super(message);
    }
}

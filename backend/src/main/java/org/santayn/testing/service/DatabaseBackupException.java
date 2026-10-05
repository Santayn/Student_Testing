package org.santayn.testing.service;

public class DatabaseBackupException extends RuntimeException {

    public DatabaseBackupException(String message) {
        super(message);
    }

    public DatabaseBackupException(String message, Throwable cause) {
        super(message, cause);
    }
}

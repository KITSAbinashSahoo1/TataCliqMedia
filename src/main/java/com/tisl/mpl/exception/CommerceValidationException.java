package com.tisl.mpl.exception;

public class CommerceValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommerceValidationException(String message) {
        super(message);
    }

    public CommerceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

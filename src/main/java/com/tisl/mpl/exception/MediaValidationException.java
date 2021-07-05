package com.tisl.mpl.exception;

public class MediaValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MediaValidationException(String message) {
        super(message);
    }

    public MediaValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

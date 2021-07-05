package com.tisl.mpl.exception;

public class CommerceServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommerceServiceException(String message) {
        super(message);
    }

    public CommerceServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

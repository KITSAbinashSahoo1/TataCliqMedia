package com.tisl.mpl.exception;

public class MediaStorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MediaStorageException(String message) {
        super(message);
    }

    public MediaStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

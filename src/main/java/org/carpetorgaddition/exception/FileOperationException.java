package org.carpetorgaddition.exception;

@SuppressWarnings("unused")
public class FileOperationException extends RuntimeException {
    public FileOperationException() {
    }

    public FileOperationException(String message) {
        super(message);
    }
}

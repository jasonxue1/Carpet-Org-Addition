package boat.carpetorgaddition.exception;

import java.io.IOException;

public class FileOperationException extends RuntimeException {
    public FileOperationException() {
    }

    public FileOperationException(String message) {
        super(message);
    }

    public FileOperationException(String message, IOException e) {
        super(message, e);
    }
}

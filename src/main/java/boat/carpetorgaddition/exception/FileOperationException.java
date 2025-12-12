package boat.carpetorgaddition.exception;

import java.io.IOException;

public class FileOperationException extends RuntimeException {
    public FileOperationException() {
    }

    public FileOperationException(String message) {
        super(message);
    }

    public FileOperationException(IOException e) {
        super(e);
    }
}

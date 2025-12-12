package boat.carpetorgaddition.exception;

public class FileOperationException extends RuntimeException {
    public FileOperationException() {
    }

    public FileOperationException(String message) {
        super(message);
    }
}

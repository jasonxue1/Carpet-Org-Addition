package boat.carpetorgaddition.exception;

/**
 * 无限循环异常
 */
public class InfiniteLoopException extends RuntimeException {
    public InfiniteLoopException() {
    }

    public InfiniteLoopException(String message) {
        super(message);
    }
}

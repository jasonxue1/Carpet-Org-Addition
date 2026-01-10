package boat.carpetorgaddition.exception;

/**
 * 无限循环异常
 */
public class InfiniteLoopException extends IllegalStateException {
    public InfiniteLoopException() {
        super("Maximum loop count exceeded, possible infinite loop detected");
    }

    public InfiniteLoopException(String message) {
        super(message);
    }
}

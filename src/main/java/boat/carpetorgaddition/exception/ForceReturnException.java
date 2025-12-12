package boat.carpetorgaddition.exception;

public class ForceReturnException extends RuntimeException {
    public static final ForceReturnException INSTANCE = new ForceReturnException();

    private ForceReturnException() {
    }
}

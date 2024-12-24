package example.exception;

public class PricingException extends RuntimeException {

    public PricingException(String message) {
        super(message);
    }
}
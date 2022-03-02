package az.kapitalbank.marketplace.exception;

public class UniqueAdditionalNumberException extends RuntimeException {

    private static final String MESSAGE =
            "Additional numbers aren't different.additionalPhoneNumber1=%s , additionalPhoneNumber2=%s";

    public UniqueAdditionalNumberException(String additionalPhoneNumber1, String additionalPhoneNumber2) {
        super(String.format(MESSAGE, additionalPhoneNumber1, additionalPhoneNumber2));
    }
}

package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UnknownLoanTerm extends RuntimeException {

    static String MESSAGE = "No such loan term. loanTerm - %s";

    public UnknownLoanTerm(int loanTerm) {
        super(String.format(MESSAGE, loanTerm));
    }
}

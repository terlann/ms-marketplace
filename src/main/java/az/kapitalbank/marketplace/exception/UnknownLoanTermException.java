package az.kapitalbank.marketplace.exception;

public class UnknownLoanTermException extends RuntimeException {

    private static final String MESSAGE = "No such loan term. loanTerm - %s";

    public UnknownLoanTermException(int loanTerm) {
        super(String.format(MESSAGE, loanTerm));
    }
}

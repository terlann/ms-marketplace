package az.kapitalbank.marketplace.exception;

public class UnknownLoanTerm extends RuntimeException {

    private static final String MESSAGE = "No such loan term. loanTerm - %s";

    public UnknownLoanTerm(int loanTerm) {
        super(String.format(MESSAGE, loanTerm));
    }
}

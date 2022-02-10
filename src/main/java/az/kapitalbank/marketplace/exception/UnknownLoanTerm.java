package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UnknownLoanTerm  extends RuntimeException {

    static final String MESSAGE = "No such loan term - [%s]";

    public UnknownLoanTerm(int loanTerm) {
        super(String.format(MESSAGE, loanTerm));
    }
}

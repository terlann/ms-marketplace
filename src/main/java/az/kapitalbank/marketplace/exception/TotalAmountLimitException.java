package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TotalAmountLimitException extends RuntimeException {

    static final String MESSAGE = "Purchase amount must be between 50 and 20000. Purchase Amount: %s";

    public TotalAmountLimitException(String purchaseAmount) {
        super(String.format(MESSAGE, purchaseAmount));
    }
}

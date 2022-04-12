package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductNotLinkedToOrder extends RuntimeException {

    static final String MESSAGE = "Product is not linked to order. productId - %s, orderNo - %s";

    public ProductNotLinkedToOrder(String productId, String orderNo) {
        super(String.format(MESSAGE, productId, orderNo));
    }
}

package az.kapitalbank.marketplace.dto;

import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderProductDeliveryInfo {
    String orderNo;
    String deliveryMethod;
    String deliveryAddress;
    String shippingLatitude;
    String shippingLongitude;
    BigDecimal totalAmount;
}

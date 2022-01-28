package az.kapitalbank.marketplace.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
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

package az.kapitalbank.marketplace.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReverseRequestDto {
    String umicoUserId;
    UUID customerId;
    String orderNo;
    BigDecimal orderAmount;
}

package az.kapitalbank.marketplace.dto.request;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReverseRequestDto {
    String umicoUserId;
    UUID customerId;
    String orderNo;
    BigDecimal orderAmount;
}

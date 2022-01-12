package az.kapitalbank.marketplace.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CustomerDetail {
    String umicoUserId;
    String userIp;
    String umicoRegistrationPhone;
    String userAgent;
    String originationLat;
    String originationLan;
    LocalDate umicoRegistrationDate;
}

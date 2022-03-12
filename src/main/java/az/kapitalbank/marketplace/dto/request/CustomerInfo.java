package az.kapitalbank.marketplace.dto.request;

import java.time.LocalDate;
import java.util.UUID;
import javax.validation.constraints.NotNull;
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
public class CustomerInfo {
    @NotNull
    String umicoUserId;
    UUID customerId;
    LocalDate registrationDate;
    String fullName;
    String mobileNumber;
    String pin;
    String email;
    Boolean isAgreement;
    String workPlace;
    LocalDate birthday;
    String additionalPhoneNumber1;
    String additionalPhoneNumber2;
    String latitude;
    String longitude;
    @NotNull
    String ip;
    @NotNull
    String userAgent;
}

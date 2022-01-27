package az.kapitalbank.marketplace.dto.request;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
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
public class CustomerInfo {
    String umicoUserId;
    UUID customerId;
    LocalDate registrationDate;
    @NotEmpty
    String fullName;
    @NotEmpty
    @Pattern(regexp = "^\\+994[0-9]{9}$")
    String mobileNumber;
    @NotEmpty
    @Pattern(regexp = "\\w{7}")
    String pin;
    @NotEmpty
    @Pattern(regexp = "^(.+)@(.+)$")
    String email;
    @NotNull
    Boolean isAgreement;
    String workPlace;
    LocalDate birthday;
    @NotEmpty
    String additionalPhoneNumber1;
    @NotEmpty
    String additionalPhoneNumber2;
    String latitude;
    String longitude;
    String ip;
    String userAgent;
}

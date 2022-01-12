package az.kapitalbank.marketplace.messaging.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FraudCheckEvent {
    String trackId;
    String fullname;
    String identityNumber;
    String mobileNumber;
    String cardPan;
    String email;
    String position;
    Boolean isKbCustomer;
    Boolean isAgreement;
    Integer loanTerm;
    BigDecimal loanAmount;
    @JsonSerialize(using = LocalDateSerializer.class)
    LocalDate birthday;
    String ip;
    String device;
    String umicoUserId;
    String employerName;
    Set<String> deliveryAddresses;
}

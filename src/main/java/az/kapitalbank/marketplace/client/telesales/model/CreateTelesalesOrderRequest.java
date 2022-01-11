package az.kapitalbank.marketplace.client.telesales.model;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public class CreateTelesalesOrderRequest {
    String pinCode;
    String phoneMob;
    String fullNameIamas;
    String position;
    String duration;
    String addressName;
    String cardPan;
    String workPlace;
    String email;
    LocalDate birthDateClient;
    Integer loanTerm;
    BigDecimal loanAmount;
    Boolean isEGovAgreement;
    Boolean isMkrAgreement;
    String orderComment;
}

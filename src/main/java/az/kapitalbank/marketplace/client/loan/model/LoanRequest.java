package az.kapitalbank.marketplace.client.loan.model;

import az.kapitalbank.marketplace.constant.FormalizationMethod;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SubProductType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LoanRequest {
    @NotNull
    String phoneNumber;
    @JsonProperty("pincode")
    String pinCode;
    @JsonProperty("fullname")
    String fullName;
    String cif;
    String address;
    Integer cardPan;
    String workplace;
    String couponCode;
    String leadComment;
    String campaignName;
    Boolean isAgreement;
    String otherProcessId;
    BigDecimal productAmount;
    Integer productDuration;
    ProductType productType;
    SubProductType subProductType;
    FormalizationMethod formalizationMethod;
    BigDecimal monthlyPayment;
    String umicoUserID;
}

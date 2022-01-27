package az.kapitalbank.marketplace.client.optimus.model.process;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@FieldDefaults(level = PRIVATE)
public class ProcessData {
    String cif;
    int period;
    int dvsOrderId;
    String salaryPan;
    MainIncome mainIncome;
    SelectedOffer selectedOffer;
    CardIssuingAmount cardIssuingAmount;
    CashIssuingAmount cashIssuingAmount;
    BigDecimal kbCardTotalDebtBurden;
    BigDecimal cashDemandedAmount;
    BigDecimal cashCommissionRate;
    BigDecimal bankGuaranteeCommissionRate;
    String pin;
    String cashCreditContractNumber;
    String initiatorUser;
    CreditDocumentsInfo creditDocumentsInfo;
}

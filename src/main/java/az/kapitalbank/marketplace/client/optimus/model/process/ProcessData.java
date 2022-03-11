package az.kapitalbank.marketplace.client.optimus.model.process;

import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = PRIVATE)
public class ProcessData {
    String cif;
    Integer period;
    Long dvsOrderId;
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
    CreateCardCreditRequest createCardCreditRequest;
}

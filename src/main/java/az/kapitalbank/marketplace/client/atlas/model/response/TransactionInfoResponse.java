package az.kapitalbank.marketplace.client.atlas.model.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
public class TransactionInfoResponse {
    BigDecimal amount;
    String authInstitutionName;
    Code code;
    Integer currency;
    BigDecimal fee;
    Long id;
    Integer installmentCount;
    BigDecimal originalAmount;
    Integer originalCurrency;
    String pan;
    ResponseCode responseCode;
    Long reverseId;
    Boolean reversed;
    String rrn;
    String terminalLocation;
    String terminalName;
    String terminalOwner;
    LocalDateTime transactionDate;
    String type;
    boolean isTransactionFound;

    public TransactionInfoResponse(boolean isTransactionFound) {
        this.isTransactionFound = isTransactionFound;
    }
}
